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
package dev.xorcery.jetty.server;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;

import java.time.Duration;

public record WebSocketsConfiguration(Configuration context)
        implements ServiceConfiguration {

    public static WebSocketsConfiguration get(Configuration configuration) {
        return new WebSocketsConfiguration(configuration.getConfiguration("jetty.server.websockets"));
    }

    public boolean isAutoFragment() {
        return context.getBoolean("autoFragment").orElseThrow(missing("autoFragment"));
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context.getString("idleTimeout").orElse("-1s"));
    }

    private int getInputBufferSize() {
        return context.getInteger("inputBufferSize").orElseThrow(missing("inputBufferSize"));
    }

    private long getMaxFrameSize() {
        return context.getLong("maxFrameSize").orElseThrow(missing("maxFrameSize"));
    }

    public int getMaxTextMessageSize() {
        return context.getInteger("maxTextMessageSize").orElseThrow(missing("maxTextMessageSize"));
    }

    private long getMaxBinaryMessageSize() {
        return context.getLong("maxBinaryMessageSize").orElseThrow(missing("maxBinaryMessageSize"));
    }

    private int getOutputBufferSize() {
        return context.getInteger("outputBufferSize").orElseThrow(missing("outputBufferSize"));
    }

    public void apply(ServerWebSocketContainer container) {
        container.setAutoFragment(isAutoFragment());
        container.setIdleTimeout(getIdleTimeout());
        container.setInputBufferSize(getInputBufferSize());
        container.setMaxFrameSize(getMaxFrameSize());
        container.setMaxTextMessageSize(getMaxTextMessageSize());
        container.setMaxBinaryMessageSize(getMaxBinaryMessageSize());
        container.setOutputBufferSize(getOutputBufferSize());
    }

}
