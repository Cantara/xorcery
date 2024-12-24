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
