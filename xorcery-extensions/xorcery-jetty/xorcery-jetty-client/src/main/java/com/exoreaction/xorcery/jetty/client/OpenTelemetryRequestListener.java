package com.exoreaction.xorcery.jetty.client;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.eclipse.jetty.client.Request;

import java.util.EventListener;

public class OpenTelemetryRequestListener
    implements Request.Listener, EventListener
{
    private static final TextMapSetter<? super Request> jettySetter =
            (carrier, key, value) -> carrier.headers(mutable -> mutable.add(key, value));
    private final Tracer tracer;

    private TextMapPropagator textMapPropagator;

    public OpenTelemetryRequestListener(OpenTelemetry openTelemetry) {
        this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
    }

    @Override
    public void onBegin(Request request) {
        Span span = tracer.spanBuilder(request.getMethod())
                .setSpanKind(SpanKind.CLIENT)
                        .startSpan();
        request.onResponseSuccess( r ->
        {
            span.setAttribute(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, r.getStatus());
            span.end();
        });
        request.onResponseFailure( (r,f)->
        {
            span.setAttribute(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, r.getStatus());
            span.end();
        });
        try (Scope scope = span.makeCurrent()) {
            textMapPropagator.inject(Context.current(), request, jettySetter);
        }
    }
}
