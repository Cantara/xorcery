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
package com.exoreaction.xorcery.service.certificates.letsencrypt;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;

public record LetsEncryptConfiguration(Configuration configuration) {

    public String getURL()
    {
        return configuration.getString("url").orElseThrow(()->new IllegalArgumentException("Missing letsencrypt.url"));
    }

    public URI getAccountLocation() {
        return configuration.getString("location").map(URI::create).orElseThrow(()->new IllegalArgumentException("Missing letsencrypt.location"));
    }

/*
    public Map<String, String> getCertificateLocations() {
        return configuration.getObjectAs("certificates", on -> JsonElement.toMap(on, JsonNode::textValue)).orElseThrow();
    }
*/
}
