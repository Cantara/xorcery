package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class JsonRequestLog
        implements RequestLog {
    private final AtomicReference<EventSink<Event<ObjectNode>>> eventSink = new AtomicReference<>();
    private LoggingMetadata loggingMetadata;

    public JsonRequestLog(LoggingMetadata loggingMetadata) {
        this.loggingMetadata = loggingMetadata;
    }

    // Backlog if no subscriber
    List<RequestLog> backlog = new ArrayList<>();

    public void setEventSink(EventSink<Event<ObjectNode>> eventSink) {
        this.eventSink.set(eventSink);

        for (RequestLog requestResponse : backlog) {
            eventSink.publishEvent((e, seq, md, ev) ->
            {
                e.metadata = md;
                e.event = ev;
            }, requestResponse.metadata, requestResponse.event);
        }
        backlog.clear();
    }

    @Override
    public void log(Request req, Response res) {

        try {
            Metadata metadata = new LoggingMetadata.Builder(new Metadata.Builder(loggingMetadata.metadata().metadata().deepCopy()))
                    .timestamp(System.currentTimeMillis())
                    .build().metadata();

            ObjectNode node = JsonNodeFactory.instance.objectNode();
            // Request
            node.set("method", node.textNode(req.getMethod()));
            node.set("uri", node.textNode(req.getRequestURI()));
            String remoteHost = req.getRemoteHost();
            if (!remoteHost.equals(""))
                node.set("remote", node.textNode(remoteHost));
            String user = req.getRemoteUser();
            if (user != null)
                node.set("user", node.textNode(user));
            String agent = req.getHeader(HttpHeader.USER_AGENT.lowerCaseName());
            if (agent != null)
                node.set("agent", node.textNode(agent));

            HttpVersion httpVersion = req.getHttpVersion();
            if (httpVersion != null)
                node.set("httpversion", node.textNode(httpVersion.asString()));

            String referer = req.getHeader(HttpHeader.REFERER.lowerCaseName());
            if (referer != null)
                node.set("referer", node.textNode(referer));

            // Response
            node.set("status", node.numberNode(res.getStatus()));
            node.set("length", node.numberNode(res.getContentLength()));

            ObjectNode event = node;

            EventSink<Event<ObjectNode>> sink = eventSink.get();
            if (sink != null) {
                sink.publishEvent((e, seq, md, ev) ->
                {
                    e.metadata = md;
                    e.event = ev;
                }, metadata, event);
            } else
            {
                backlog.add(new RequestLog(metadata, event));
            }
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not log request", e);
        }
    }

    record RequestLog(Metadata metadata, ObjectNode event){}
}
