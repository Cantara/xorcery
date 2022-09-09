package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
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
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class JsonRequestLog
        implements RequestLog {
    private LoggingMetadata loggingMetadata;
    private EventSink<WithMetadata<ObjectNode>> eventSink;

    public JsonRequestLog(LoggingMetadata loggingMetadata, EventSink<WithMetadata<ObjectNode>> eventSink) {
        this.loggingMetadata = loggingMetadata;
        this.eventSink = eventSink;
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

            boolean isPublished = eventSink.tryPublishEvent((item, s, md, e) ->
            {
                item.set(md, e);
            }, metadata, event);

            if (!isPublished) {
                LogManager.getLogger(getClass()).warn("Pending request log full, dropping requests");
            }
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not log request", e);
        }
    }
}
