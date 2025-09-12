package dev.xorcery.reactivestreams.server;

import io.opentelemetry.context.Context;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;

import java.util.Map;

public interface ServerSubProtocol
    extends AutoCloseable
{
    void close();

    Session.Listener.AutoDemanding createSubProtocolHandler(
            ServerUpgradeRequest serverUpgradeRequest,
            ServerUpgradeResponse serverUpgradeResponse,
            String clientHost,
            String path,
            Map<String, String> pathParameters,
            int clientMaxBinaryMessageSize,
            Context context);
}
