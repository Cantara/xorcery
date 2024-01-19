package com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp.jdk;

import io.opentelemetry.exporter.internal.auth.Authenticator;
import io.opentelemetry.exporter.internal.compression.Compressor;
import io.opentelemetry.exporter.internal.http.HttpSender;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This only exists because the official JDK HttpSender does not specify HTTP protocol
 *
 * @author rickardoberg
 * @since 18/01/2024
 */
public class JdkHttpSenderProvider
        implements io.opentelemetry.exporter.internal.http.HttpSenderProvider {

    @Override
    public HttpSender createSender(
            String endpoint,
            @Nullable Compressor compressor,
            boolean exportAsJson,
            String contentType,
            long timeoutNanos,
            long connectTimeout,
            Supplier<Map<String, List<String>>> headerSupplier,
            @Nullable Authenticator authenticator,
            @Nullable RetryPolicy retryPolicy,
            @Nullable SSLContext sslContext,
            @Nullable X509TrustManager trustManager) {
        return new JdkHttpSender(
                endpoint,
                compressor,
                exportAsJson,
                contentType,
                timeoutNanos,
                connectTimeout,
                headerSupplier,
                retryPolicy,
                sslContext);
    }
}
