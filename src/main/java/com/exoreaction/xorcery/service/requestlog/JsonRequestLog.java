package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class JsonRequestLog
        implements RequestLog {
    private final AtomicReference<EventSink<Event<ObjectNode>>> eventSink = new AtomicReference<>();
    private LoggingMetadata loggingMetadata;

    public JsonRequestLog(LoggingMetadata loggingMetadata) {
        this.loggingMetadata = loggingMetadata;
    }

    public void setEventSink(EventSink<Event<ObjectNode>> eventSink) {
        this.eventSink.set(eventSink);
    }

    @Override
    public void log(Request request, Response response) {
        EventSink<Event<ObjectNode>> sink = eventSink.get();
        if (sink != null) {
            sink.publishEvent((event, seq, req, res) ->
            {
                event.metadata = new LoggingMetadata.Builder(new Metadata.Builder(loggingMetadata.metadata().metadata().deepCopy()))
                        .timestamp(System.currentTimeMillis())
                        .build().metadata();

                ObjectNode node = JsonNodeFactory.instance.objectNode();
                // Request
                node.set("method", node.textNode(req.getMethod()));
                node.set("uri", node.textNode(req.getRequestURI()));

                // Response
                node.set("status", node.numberNode(res.getStatus()));
                node.set("length", node.numberNode(res.getContentLength()));

                event.event = node;
            }, request, response);
        }

    }
}
