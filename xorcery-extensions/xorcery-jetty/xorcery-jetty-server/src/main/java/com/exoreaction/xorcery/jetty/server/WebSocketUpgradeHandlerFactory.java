package com.exoreaction.xorcery.jetty.server;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
@Priority(6)
public class WebSocketUpgradeHandlerFactory
    implements Factory<WebSocketUpgradeHandler>
{
    private final WebSocketUpgradeHandler webSocketHandler;

    @Inject
    public WebSocketUpgradeHandlerFactory(Configuration configuration, Server server) {
        WebSocketsConfiguration webSocketsConfiguration = WebSocketsConfiguration.get(configuration);
        webSocketHandler = WebSocketUpgradeHandler.from(server, webSocketsConfiguration::apply);
    }

    @Named("jetty.server.websockets")
    @Singleton
    @Override
    public WebSocketUpgradeHandler provide() {
        return webSocketHandler;
    }

    @Override
    public void dispose(WebSocketUpgradeHandler instance) {

    }
}
