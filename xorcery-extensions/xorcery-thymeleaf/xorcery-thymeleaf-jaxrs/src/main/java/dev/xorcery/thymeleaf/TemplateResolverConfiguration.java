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
package dev.xorcery.thymeleaf;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record TemplateResolverConfiguration(Configuration configuration) {

    public TemplateMode getTemplateMode() {
        return configuration.getEnum("templateMode", TemplateMode.class)
                .orElse(TemplateMode.HTML);
    }

    public String getPrefix() {
        return configuration.getString("prefix")
                .orElse("WEB-INF/thymeleaf/templates/");
    }

    public String getSuffix() {
        return configuration.getString("suffix")
                .orElse(".html");
    }

    public boolean getCacheable() {
        return configuration.getBoolean("cacheable")
                .orElse(false);
    }

    public Duration getCacheTTL() {
        return Duration.parse("PT" + configuration.getString("cacheTTL").orElse("1h"));
    }

    public Set<String> getCacheablePatterns() {
        return Set.copyOf(configuration
                .getListAs("cacheablePatterns", JsonNode::asText)
                .orElse(Collections.emptyList()));
    }

    public String getCharacterEncoding() {
        return configuration.getString("encoding").orElse(StandardCharsets.UTF_8.name());
    }

    public Set<String> getNonCacheablePatterns() {
        return Set.copyOf(configuration
                .getListAs("nonCacheablePatterns", JsonNode::asText)
                .orElse(Collections.emptyList()));
    }

    public boolean isCheckExistence() {
        return configuration.getBoolean("checkExistence").orElse(false);
    }

    public Set<String> getResolvablePatterns() {
        return Set.copyOf(configuration
                .getListAs("resolvablePatterns", JsonNode::asText)
                .orElse(Collections.emptyList()));
    }

    public Map<String, String> getTemplateAliases() {
        return configuration.getObjectAs("aliases", JsonElement.toMap(JsonNode::asText))
                .orElse(Collections.emptyMap());
    }

    public boolean isDecoupledLogic() {
        return configuration.getBoolean("decoupledLogic").orElse(false);
    }
}
