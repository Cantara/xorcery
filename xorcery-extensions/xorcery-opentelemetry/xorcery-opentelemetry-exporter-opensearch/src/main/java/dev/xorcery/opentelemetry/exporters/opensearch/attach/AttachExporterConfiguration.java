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
package dev.xorcery.opentelemetry.exporters.opensearch.attach;

import dev.xorcery.configuration.Configuration;

import java.net.URL;

public record AttachExporterConfiguration(Configuration configuration) {
    public static AttachExporterConfiguration get(Configuration configuration) {
        return new AttachExporterConfiguration(configuration.getConfiguration("opentelemetry.exporters.opensearch.attach"));
    }

    public URL getHost() {
        return configuration.getURL("host").orElseThrow(Configuration.missing("host"));
    }
}
