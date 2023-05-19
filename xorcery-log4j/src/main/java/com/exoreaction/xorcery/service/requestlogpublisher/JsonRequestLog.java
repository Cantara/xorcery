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
package com.exoreaction.xorcery.service.requestlogpublisher;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.log4jpublisher.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

@Singleton
public class JsonRequestLog
        implements RequestLog {
    private LoggingMetadata loggingMetadata;
    private RequestLogPublisherService.RequestLogPublisher requestLogPublisher;

    public JsonRequestLog(LoggingMetadata loggingMetadata, RequestLogPublisherService.RequestLogPublisher requestLogPublisher) {
        this.loggingMetadata = loggingMetadata;
        this.requestLogPublisher = requestLogPublisher;
    }

    @Override
    public void log(Request req, Response res) {

        try {
            Metadata metadata = new LoggingMetadata.Builder(new Metadata.Builder(loggingMetadata.context().metadata().deepCopy()))
                    .timestamp(System.currentTimeMillis())
                    .build().context();

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

            requestLogPublisher.send(new WithMetadata<>(metadata, node));
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not log request", e);
        }
    }
}
