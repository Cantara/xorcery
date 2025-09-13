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
import dev.xorcery.reactivestreams.spi.MessageReader;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Function;

import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM;

public class ServerSubscriberSubProtocol<SUBSCRIBE>
        implements ServerSubProtocol {

    private final ServerWebSocketOptions options;
    private final Class<? super SUBSCRIBE> subscribeType;
    private final Function<Flux<SUBSCRIBE>, Disposable> subscriber;
    private final ServerWebSocketStreamsService serverWebSocketStreamsService;

    public ServerSubscriberSubProtocol(
            ServerWebSocketOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            Function<Flux<SUBSCRIBE>, Disposable> subscriber,
            ServerWebSocketStreamsService serverWebSocketStreamsService) {
        this.options = options;
        this.subscribeType = subscribeType;
        this.subscriber = subscriber;
        this.serverWebSocketStreamsService = serverWebSocketStreamsService;
    }

    @Override
    public Session.Listener.AutoDemanding createSubProtocolHandler(
            ServerUpgradeRequest serverUpgradeRequest,
            ServerUpgradeResponse serverUpgradeResponse,
            String clientHost,
            String path,
            Map<String, String> pathParameters,
            int clientMaxBinaryMessageSize,
            Context context) {
        MessageReader<SUBSCRIBE> reader = (MessageReader<SUBSCRIBE>) serverWebSocketStreamsService.getReader(subscribeType, serverUpgradeRequest, serverUpgradeResponse);
        if (reader == null)
            return null;

        Attributes attributes = Attributes.builder()
                .put(MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME, path)
                .put(MessagingIncubatingAttributes.MESSAGING_SYSTEM, XORCERY_MESSAGING_SYSTEM)
                .put(ClientAttributes.CLIENT_ADDRESS, clientHost)
                .build();


        return new ServerSubscriberSubProtocolHandler<SUBSCRIBE>(
                serverWebSocketStreamsService.getConnectionCounter(),
                options,
                reader,
                subscriber,
                path,
                pathParameters,
                clientMaxBinaryMessageSize,
                serverWebSocketStreamsService.getLoggerContext().getLogger(ServerSubscriberSubProtocolHandler.class),
                serverWebSocketStreamsService.getTracer(),
                serverWebSocketStreamsService.getMeter(),
                context,
                attributes
                );
    }

    @Override
    public void close() {
        // TODO
    }
}
