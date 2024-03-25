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
package com.exoreaction.xorcery.opentelemetry.jersey.server.resources;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.*;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 17/01/2024
 */
@Provider
public class OpenTelemetryTracerFilter
        implements ContainerRequestFilter, ContainerResponseFilter {
    private final TextMapPropagator textMapPropagator;

    private static final TextMapGetter<ContainerRequestContext> jerseyGetter =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(ContainerRequestContext context) {
                    return context.getHeaders().keySet();
                }

                @Override
                public String get(ContainerRequestContext context, String key) {
                    return context.getHeaderString(key);
                }
            };
    private static final TextMapGetter<Request> jettyGetter =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(Request context) {
                    return context.getHeaders().getFieldNamesCollection();
                }

                @Override
                public String get(Request context, String key) {
                    return context.getHeaders().get(key);
                }
            };
    private final Tracer tracer;

    private final Map<String, String> attributes;
    private final Logger logger;

    @Inject
    public OpenTelemetryTracerFilter(OpenTelemetry openTelemetry, Server server, Configuration configuration, Logger logger) {
        this.logger = logger;
        OpenTelemetryJerseyConfiguration jerseyConfiguration = OpenTelemetryJerseyConfiguration.get(configuration);

        this.tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();

        attributes = jerseyConfiguration.getAttributes();

        server.setRequestLog(new OpenTelemetryRequestLog());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            Context context = textMapPropagator.extract(Context.current(), requestContext, jerseyGetter);
            context.makeCurrent();
            var uriInfo = requestContext.getUriInfo();
            String route = getHttpRoute(uriInfo);

            Span span = tracer.spanBuilder(requestContext.getMethod() + (route != null ? " " + route : ""))
                    .setParent(context)
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan();
            requestContext.setProperty("opentelemetry.span", span);
        } catch (Throwable e) {
            logger.error("Could not create span", e);
        }
    }


    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            Span span = (Span) requestContext.getProperty("opentelemetry.span");
            var uriInfo = requestContext.getUriInfo();
            String route = getHttpRoute(uriInfo);
            if (span == null) {
                Context context = textMapPropagator.extract(Context.current(), requestContext, jerseyGetter);
                span = tracer.spanBuilder(requestContext.getMethod() + (route != null ? " " + route : ""))
                        .setParent(context)
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan();
                requestContext.setProperty("opentelemetry.span", span);

            }
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                switch (attribute.getValue()) {
                    case "http.request.method" -> span.setAttribute(attribute.getKey(), requestContext.getMethod());
                    case "url.full" -> span.setAttribute(attribute.getKey(), uriInfo.getRequestUri().toASCIIString());
                    case "http.route" -> {
                        if (route != null)
                            span.setAttribute(attribute.getKey(), route);
                    }
                    case "http.response.status_code" ->
                            span.setAttribute(attribute.getKey(), responseContext.getStatus());
                    case "enduser.id" -> {
                        Principal p = requestContext.getSecurityContext().getUserPrincipal();
                        if (p != null)
                            span.setAttribute(attribute.getKey(), p.getName());
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Could not create span", e);
        }
    }

    private String getHttpRoute(UriInfo uriInfo) {
        if (uriInfo instanceof ExtendedUriInfo extendedUriInfo) {
            StringBuilder httpRoute = new StringBuilder();
            List<UriTemplate> matchedTemplates = extendedUriInfo.getMatchedTemplates();

            if (matchedTemplates == null || matchedTemplates.isEmpty()) {
                return null;
            }

            for (int i = matchedTemplates.size() - 1; i >= 0; i--) {
                String uriTemplate = matchedTemplates.get(i).getTemplate();
                httpRoute.append(uriTemplate);
            }
            return httpRoute.toString();
        } else {
            return null;
        }
    }

    private class OpenTelemetryRequestLog
            implements RequestLog {

        @Override
        public void log(Request request, Response response) {
            try {
                Span span = (Span) request.getAttribute("opentelemetry.span");
                if (span == null) {
                    // Jetty request

                    Context context = textMapPropagator.extract(Context.current(), request, jettyGetter);
                    span = tracer.spanBuilder(request.getMethod())
                            .setParent(context)
                            .setStartTimestamp(request.getBeginNanoTime(), TimeUnit.NANOSECONDS)
                            .setSpanKind(SpanKind.SERVER)
                            .startSpan();

                    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                        switch (attribute.getValue()) {
                            case "http.request.method" -> span.setAttribute(attribute.getKey(), request.getMethod());
                            case "url.full" ->
                                    span.setAttribute(attribute.getKey(), request.getHttpURI().toURI().toASCIIString());
                            case "http.response.status_code" ->
                                    span.setAttribute(attribute.getKey(), response.getStatus());
                        }
                    }
                }

                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    if (attribute.getValue().equals("http.response.body.size")) {
                        String lengthStr = response.getHeaders().get(HttpHeader.CONTENT_LENGTH);
                        if (lengthStr != null)
                        {
                            var length = Long.valueOf(lengthStr);
                            if (length > 0)
                                span.setAttribute(attribute.getKey(), length);
                        }
                    }
                }
                span.end();
            } catch (Throwable e) {
                logger.error("Could not create span", e);
            }
        }
    }
}
