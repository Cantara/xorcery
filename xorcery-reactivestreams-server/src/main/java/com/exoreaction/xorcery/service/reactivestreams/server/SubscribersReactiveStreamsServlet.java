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
package com.exoreaction.xorcery.service.reactivestreams.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class SubscribersReactiveStreamsServlet
        extends JettyWebSocketServlet {
    private final Configuration configuration;
    private final Function<String, Object> subscriberWebSocketEndpointFactory;

    public SubscribersReactiveStreamsServlet(Configuration configuration,
                                             Function<String, Object> subscriberWebSocketEndpointFactory) {

        this.subscriberWebSocketEndpointFactory = subscriberWebSocketEndpointFactory;
        this.configuration = configuration;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.setCreator((jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
                subscriberWebSocketEndpointFactory.apply(jettyServerUpgradeRequest.getRequestPath().substring( "/streams/subscribers/".length())));
    }
}
