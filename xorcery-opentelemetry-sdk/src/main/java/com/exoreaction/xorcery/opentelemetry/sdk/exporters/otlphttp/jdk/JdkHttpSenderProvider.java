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
