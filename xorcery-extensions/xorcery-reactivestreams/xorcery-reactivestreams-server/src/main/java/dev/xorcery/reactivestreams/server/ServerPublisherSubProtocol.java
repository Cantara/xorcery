package dev.xorcery.reactivestreams.server;

import io.opentelemetry.context.Context;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.reactivestreams.Publisher;

import java.util.Map;

public class ServerPublisherSubProtocol<PUBLISH> implements ServerSubProtocol {
    private final Class<? super PUBLISH> publishType;
    private final Publisher<PUBLISH> publisher;
    private final ServerWebSocketStreamsService serverWebSocketStreamsService;

    public ServerPublisherSubProtocol(Class<? super PUBLISH> publishType, Publisher<PUBLISH> publisher, ServerWebSocketStreamsService serverWebSocketStreamsService) {
        this.publishType = publishType;
        this.publisher = publisher;
        this.serverWebSocketStreamsService = serverWebSocketStreamsService;
    }

    @Override
    public Session.Listener.AutoDemanding createSubProtocolHandler(ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse, String clientHost, String path, Map<String, String> pathParameters, int clientMaxBinaryMessageSize, Context context) {
        return null;
    }

    @Override
    public void close() {

    }
}
