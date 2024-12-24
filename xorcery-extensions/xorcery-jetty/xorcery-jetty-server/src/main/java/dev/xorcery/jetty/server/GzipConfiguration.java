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

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record GzipConfiguration(Configuration configuration) {

    public static GzipConfiguration get(Configuration configuration) {
        return new GzipConfiguration(configuration.getConfiguration("jetty.server.gzip"));
    }

    public List<String> getExcludedMediaTypes()
    {
        return configuration.getListAs("excluded.mediatypes", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getExcludedMethods()
    {
        return configuration.getListAs("excluded.methods", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getExcludedPaths()
    {
        return configuration.getListAs("excluded.paths", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getIncludedMediaTypes()
    {
        return configuration.getListAs("included.mediatypes", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getIncludedMethods()
    {
        return configuration.getListAs("included.methods", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getIncludedPaths()
    {
        return configuration.getListAs("included.paths", JsonNode::asText).orElse(Collections.emptyList());
    }

    public Optional<Integer> getMinGzipSize() {
        return configuration.getInteger("minGzipSize");
    }

    public Optional<Boolean> isSyncFlush() {
        return configuration.getBoolean("syncFlush");
    }
}
