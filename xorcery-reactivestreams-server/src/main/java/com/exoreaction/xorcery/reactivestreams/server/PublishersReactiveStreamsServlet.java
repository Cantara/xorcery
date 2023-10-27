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
package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.configuration.Configuration;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class PublishersReactiveStreamsServlet
        extends JettyWebSocketServlet {
    private final ReactiveStreamsServerConfiguration configuration;
    private final Function<String, Object> publisherWebSocketEndpointFactory;

    public PublishersReactiveStreamsServlet(ReactiveStreamsServerConfiguration configuration,
                                            Function<String, Object> publisherWebSocketEndpointFactory) {

        this.publisherWebSocketEndpointFactory = publisherWebSocketEndpointFactory;
        this.configuration = configuration;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(configuration.getMaxTextMessageSize());
        factory.setIdleTimeout(configuration.getIdleTimeout());

        factory.setCreator((jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
                {
                    return publisherWebSocketEndpointFactory.apply(jettyServerUpgradeRequest.getRequestPath().substring( "/streams/publishers/".length()));
                });
    }
}
