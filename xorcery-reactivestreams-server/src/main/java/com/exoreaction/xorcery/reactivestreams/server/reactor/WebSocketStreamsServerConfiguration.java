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
package com.exoreaction.xorcery.reactivestreams.server.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;

import java.net.URI;
import java.time.Duration;

public record WebSocketStreamsServerConfiguration(Configuration context)
        implements ServiceConfiguration {

    public static WebSocketStreamsServerConfiguration get(Configuration configuration)
    {
        return new WebSocketStreamsServerConfiguration(configuration.getConfiguration("reactivestreams.server"));
    }

    public URI getURI() {
        return context.getURI("uri").orElseThrow(missing("reactivestreams.server.uri"));
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context.getString("idleTimeout").orElse("-1s"));
    }

    public int getMaxTextMessageSize() {
        return context.getInteger("maxTextMessageSize").orElse(1048576);
    }
}
