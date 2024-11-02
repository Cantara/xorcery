package dev.xorcery.reactivestreams.client;

import dev.xorcery.configuration.Configuration;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.time.Duration;

public record ClientWebSocketStreamConfiguration(Configuration configuration) {
    public static ClientWebSocketStreamConfiguration get(Configuration configuration) {
        return new ClientWebSocketStreamConfiguration(configuration.getConfiguration("reactivestreams.client"));
    }

    public Duration getConnectTimeout() {
        return Duration.parse("PT" + configuration.getString("connectTimeout").orElse("5s"));
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + configuration.getString("idleTimeout").orElse("-1s"));
    }

    public boolean isAutoFragment() {
        return configuration.getBoolean("autoFragment").orElse(true);
    }

    public int getMaxTextMessageSize() {
        return configuration.getInteger("maxTextMessageSize").orElse(1048576);
    }

    public int getMaxBinaryMessageSize() {
        return configuration.getInteger("maxBinaryMessageSize").orElse(1048576);
    }

    public int getMaxFrameSize() {
        return configuration.getInteger("maxFrameSize").orElse(65536);
    }

    public int getInputBufferSize() {
        return configuration.getInteger("inputBufferSize").orElse(4096);
    }

    public int getOutputBufferSize() {
        return configuration.getInteger("maxTextMessageSize").orElse(4096);
    }
    
    public void configure(WebSocketClient webSocketClient)
    {
        webSocketClient.setStopAtShutdown(false);
        webSocketClient.setIdleTimeout(getIdleTimeout());
        webSocketClient.setConnectTimeout(getConnectTimeout().toMillis());
        webSocketClient.setAutoFragment(isAutoFragment());
        webSocketClient.setMaxTextMessageSize(getMaxTextMessageSize());
        webSocketClient.setMaxBinaryMessageSize(getMaxBinaryMessageSize());
        webSocketClient.setMaxFrameSize(getMaxFrameSize());
        webSocketClient.setInputBufferSize(getInputBufferSize());
        webSocketClient.setOutputBufferSize(getOutputBufferSize());
    }
}
