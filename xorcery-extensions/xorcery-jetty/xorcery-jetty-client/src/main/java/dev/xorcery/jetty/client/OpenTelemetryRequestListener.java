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
package dev.xorcery.jetty.client;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.SchemaUrls;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import org.eclipse.jetty.client.Request;

import java.util.EventListener;

public class OpenTelemetryRequestListener
        implements Request.Listener, EventListener {
    private static final TextMapSetter<? super Request> jettySetter =
            (carrier, key, value) -> carrier.headers(mutable -> mutable.add(key, value));
    private final Tracer tracer;

    private TextMapPropagator textMapPropagator;

    public OpenTelemetryRequestListener(OpenTelemetry openTelemetry) {
        this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
    }

    @Override
    public void onBegin(Request request) {
        Span span = tracer.spanBuilder(request.getMethod())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, request.getMethod())
                .setAttribute(ServerAttributes.SERVER_ADDRESS, request.getHost())
                .setAttribute(UrlAttributes.URL_FULL, request.getURI().toASCIIString())
                .startSpan();
        request.onResponseSuccess(r ->
        {
            span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, r.getStatus());
            span.end();
        });
        request.onResponseFailure((r, f) ->
        {
            span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, r.getStatus());
            span.end();
        });
        try (Scope scope = span.makeCurrent()) {
            textMapPropagator.inject(Context.current(), request, jettySetter);
        }
    }
}
