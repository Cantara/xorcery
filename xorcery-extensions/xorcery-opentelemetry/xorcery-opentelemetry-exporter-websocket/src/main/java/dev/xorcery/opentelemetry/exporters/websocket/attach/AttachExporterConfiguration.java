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
package dev.xorcery.opentelemetry.exporters.websocket.attach;

import dev.xorcery.configuration.Configuration;

import java.net.URI;

public record AttachExporterConfiguration(Configuration configuration) {
    public static AttachExporterConfiguration get(Configuration configuration) {
        return new AttachExporterConfiguration(configuration.getConfiguration("opentelemetry.exporters.websocket.attach"));
    }
    public URI getCollectorUri() {
        URI host =  configuration.getURI("host").orElseThrow(Configuration.missing("host"));
        if (host.getPath().equals("/"))
        {
            return host.resolve("collector/v1");
        } else
        {
            return host;
        }
    }

    public boolean isOptimizeResource()
    {
        return configuration.getBoolean("optimizeResource").orElse(false);
    }
}
