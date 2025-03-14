/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.opentelemetry.exporters.otlphttp.jdk;

import io.opentelemetry.exporter.internal.compression.Compressor;
import io.opentelemetry.exporter.internal.http.HttpSender;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */
public final class JdkHttpSender implements HttpSender {

    private static final Set<Integer> retryableStatusCodes = Set.of(429, 502, 503, 504);

    private static final ThreadLocal<JdkHttpSender.NoCopyByteArrayOutputStream> threadLocalBaos =
            ThreadLocal.withInitial(JdkHttpSender.NoCopyByteArrayOutputStream::new);
    private static final ThreadLocal<JdkHttpSender.ByteBufferPool> threadLocalByteBufPool =
            ThreadLocal.withInitial(JdkHttpSender.ByteBufferPool::new);

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final HttpClient client;
    private final URI uri;
    @Nullable
    private final Compressor compressor;
    private final boolean exportAsJson;
    private final String contentType;
    private final long timeoutNanos;
    private final Supplier<Map<String, List<String>>> headerSupplier;
    @Nullable private final RetryPolicy retryPolicy;

    // Visible for testing
    JdkHttpSender(
            HttpClient client,
            String endpoint,
            @Nullable Compressor compressor,
            boolean exportAsJson,
            String contentType,
            long timeoutNanos,
            Supplier<Map<String, List<String>>> headerSupplier,
            @Nullable RetryPolicy retryPolicy) {
        this.client = client;
        try {
            this.uri = new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        this.compressor = compressor;
        this.exportAsJson = exportAsJson;
        this.contentType = contentType;
        this.timeoutNanos = timeoutNanos;
        this.headerSupplier = headerSupplier;
        this.retryPolicy = retryPolicy;
    }

    JdkHttpSender(
            String endpoint,
            @Nullable Compressor compressor,
            boolean exportAsJson,
            String contentType,
            long timeoutNanos,
            long connectTimeoutNanos,
            Supplier<Map<String, List<String>>> headerSupplier,
            @Nullable RetryPolicy retryPolicy,
            @Nullable SSLContext sslContext) {
        this(
                configureClient(sslContext, connectTimeoutNanos),
                endpoint,
                compressor,
                exportAsJson,
                contentType,
                timeoutNanos,
                headerSupplier,
                retryPolicy);
    }

    private static HttpClient configureClient(
            @Nullable SSLContext sslContext, long connectionTimeoutNanos) {
        HttpClient.Builder builder =
                HttpClient.newBuilder().connectTimeout(Duration.ofNanos(connectionTimeoutNanos));
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        builder.version(HttpClient.Version.HTTP_1_1);
        return builder.build();
    }

    @Override
    public void send(
            Marshaler marshaler,
            int contentLength,
            Consumer<Response> onResponse,
            Consumer<Throwable> onError) {
        CompletableFuture<HttpResponse<byte[]>> unused =
                CompletableFuture.supplyAsync(
                                () -> {
                                    try {
                                        return sendInternal(marshaler);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                },
                                executorService)
                        .whenComplete(
                                (httpResponse, throwable) -> {
                                    if (throwable != null) {
                                        onError.accept(throwable);
                                        return;
                                    }
                                    onResponse.accept(toHttpResponse(httpResponse));
                                });
    }

    // Visible for testing
    HttpResponse<byte[]> sendInternal(Marshaler marshaler) throws IOException {
        long startTimeNanos = System.nanoTime();
        HttpRequest.Builder requestBuilder =
                HttpRequest.newBuilder().uri(uri).timeout(Duration.ofNanos(timeoutNanos));
        Map<String, List<String>> headers = headerSupplier.get();
        if (headers != null) {
            headers.forEach((key, values) -> values.forEach(value -> requestBuilder.header(key, value)));
        }
        requestBuilder.header("Content-Type", contentType);

        JdkHttpSender.NoCopyByteArrayOutputStream os = threadLocalBaos.get();
        os.reset();
        if (compressor != null) {
            requestBuilder.header("Content-Encoding", compressor.getEncoding());
            try (OutputStream compressed = compressor.compress(os)) {
                write(marshaler, compressed);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            write(marshaler, os);
        }

        JdkHttpSender.ByteBufferPool byteBufferPool = threadLocalByteBufPool.get();
        requestBuilder.POST(new BodyPublisher(os.buf(), os.size(), byteBufferPool::getBuffer));

        // If no retry policy, short circuit
        if (retryPolicy == null) {
            return sendRequest(requestBuilder, byteBufferPool);
        }

        long attempt = 0;
        long nextBackoffNanos = retryPolicy.getInitialBackoff().toNanos();
        HttpResponse<byte[]> httpResponse = null;
        IOException exception = null;
        do {
            if (attempt > 0) {
                // Compute and sleep for backoff
                long upperBoundNanos = Math.min(nextBackoffNanos, retryPolicy.getMaxBackoff().toNanos());
                long backoffNanos = ThreadLocalRandom.current().nextLong(upperBoundNanos);
                nextBackoffNanos = (long) (nextBackoffNanos * retryPolicy.getBackoffMultiplier());
                try {
                    TimeUnit.NANOSECONDS.sleep(backoffNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // Break out and return response or throw
                }
                // If after sleeping we've exceeded timeoutNanos, break out and return response or throw
                if ((System.nanoTime() - startTimeNanos) >= timeoutNanos) {
                    break;
                }
            }

            attempt++;
            requestBuilder.timeout(Duration.ofNanos(timeoutNanos - (System.nanoTime() - startTimeNanos)));
            try {
                httpResponse = sendRequest(requestBuilder, byteBufferPool);
            } catch (IOException e) {
                exception = e;
            }

            if (httpResponse != null && !retryableStatusCodes.contains(httpResponse.statusCode())) {
                return httpResponse;
            }
            if (exception != null && !isRetryableException(exception)) {
                throw exception;
            }
        } while (attempt < retryPolicy.getMaxAttempts());

        if (httpResponse != null) {
            return httpResponse;
        }
        throw exception;
    }

    private void write(Marshaler marshaler, OutputStream os) throws IOException {
        if (exportAsJson) {
            marshaler.writeJsonTo(os);
        } else {
            marshaler.writeBinaryTo(os);
        }
    }

    private HttpResponse<byte[]> sendRequest(
            HttpRequest.Builder requestBuilder, JdkHttpSender.ByteBufferPool byteBufferPool) throws IOException {
        try {
            return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            byteBufferPool.resetPool();
        }
    }

    private static boolean isRetryableException(IOException throwable) {
        // Almost all IOExceptions we've encountered are transient retryable, so we opt out of specific
        // IOExceptions that are unlikely to resolve rather than opting in.
        // Known retryable IOException messages: "Connection reset", "/{remote ip}:{remote port} GOAWAY
        // received"
        // Known retryable HttpTimeoutException messages: "request timed out"
        // Known retryable HttpConnectTimeoutException messages: "HTTP connect timed out"
        return !(throwable instanceof SSLException);
    }

    private static class NoCopyByteArrayOutputStream extends ByteArrayOutputStream {
        NoCopyByteArrayOutputStream() {
            super(retryableStatusCodes.size());
        }

        private byte[] buf() {
            return buf;
        }
    }

    private static Response toHttpResponse(HttpResponse<byte[]> response) {
        return new Response() {
            @Override
            public int statusCode() {
                return response.statusCode();
            }

            @Override
            public String statusMessage() {
                return String.valueOf(response.statusCode());
            }

            @Override
            public byte[] responseBody() {
                return response.body();
            }
        };
    }

    private static class ByteBufferPool {

        // TODO: make configurable?
        private static final int BUF_SIZE = 16 * 1024;

        private final ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<ByteBuffer> out = new ConcurrentLinkedQueue<>();

        private ByteBuffer getBuffer() {
            ByteBuffer buffer = pool.poll();
            if (buffer == null) {
                buffer = ByteBuffer.allocate(BUF_SIZE);
            }
            out.offer(buffer);
            return buffer;
        }

        private void resetPool() {
            ByteBuffer buf = out.poll();
            while (buf != null) {
                pool.offer(buf);
                buf = out.poll();
            }
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        executorService.shutdown();
        return CompletableResultCode.ofSuccess();
    }
}