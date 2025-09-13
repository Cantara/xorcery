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
package dev.xorcery.reactivestreams.server;

import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.reactivestreams.Publisher;

import java.util.Map;

import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM;

public class ServerPublisherSubProtocol<PUBLISH> implements ServerSubProtocol {
    private final ServerWebSocketOptions options;
    private final Class<? super PUBLISH> publishType;
    private final Publisher<PUBLISH> publisher;
    private final ServerWebSocketStreamsService serverWebSocketStreamsService;

    public ServerPublisherSubProtocol(
            ServerWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Publisher<PUBLISH> publisher,
            ServerWebSocketStreamsService serverWebSocketStreamsService) {
        this.options = options;
        this.publishType = publishType;
        this.publisher = publisher;
        this.serverWebSocketStreamsService = serverWebSocketStreamsService;
    }

    @Override
    public Session.Listener.AutoDemanding createSubProtocolHandler(ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse, String clientHost, String path, Map<String, String> pathParameters, int clientMaxBinaryMessageSize, Context context) {
        MessageWriter<PUBLISH> writer = (MessageWriter<PUBLISH>) serverWebSocketStreamsService.getWriter(publishType, serverUpgradeRequest, serverUpgradeResponse);
        if (writer == null)
            return null;

        Attributes attributes = Attributes.builder()
                .put(MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME, path)
                .put(MessagingIncubatingAttributes.MESSAGING_SYSTEM, XORCERY_MESSAGING_SYSTEM)
                .put(ClientAttributes.CLIENT_ADDRESS, clientHost)
                .build();

        return new ServerPublisherSubProtocolHandler<PUBLISH>(
                serverWebSocketStreamsService.getConnectionCounter(),
                options,
                writer,
                publisher,
                path,
                pathParameters,
                clientMaxBinaryMessageSize,
                serverWebSocketStreamsService.getFlushingExecutors(),
                serverWebSocketStreamsService.getByteBufferPool(),
                serverWebSocketStreamsService.getLoggerContext().getLogger(ServerPublisherSubProtocolHandler.class),
                serverWebSocketStreamsService.getTracer(),
                serverWebSocketStreamsService.getMeter(),
                context,
                attributes
        );
    }

    @Override
    public void close() {

    }
}
