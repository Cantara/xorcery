package dev.xorcery.reactivestreams.server;

import io.opentelemetry.context.Context;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Function;

public class ServerSubscriberWithResultSubProtocol<SUBSCRIBE, RESULT> implements ServerSubProtocol {
    private final Class<? super SUBSCRIBE> subscribeType;
    private final Class<? super RESULT> resultType;
    private final Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeAndReturnResult;
    private final ServerWebSocketStreamsService serverWebSocketStreamsService;

    public ServerSubscriberWithResultSubProtocol(Class<? super SUBSCRIBE> subscribeType, Class<? super RESULT> resultType, Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeAndReturnResult, ServerWebSocketStreamsService serverWebSocketStreamsService) {
        this.subscribeType = subscribeType;
        this.resultType = resultType;
        this.subscribeAndReturnResult = subscribeAndReturnResult;
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
