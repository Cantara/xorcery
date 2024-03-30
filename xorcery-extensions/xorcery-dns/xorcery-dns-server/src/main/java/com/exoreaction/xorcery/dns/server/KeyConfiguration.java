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
package com.exoreaction.xorcery.dns.server;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record KeyConfiguration(ObjectNode json)
        implements JsonElement {
    public String getName() {
        return getString("name").orElseThrow(() -> new IllegalArgumentException("No key name set"));
    }

    public String getSecret() {
        return getString("secret").orElseThrow(() -> new IllegalArgumentException("No key secret set"));
    }

    public String getAlgorithm() {
        return getString("algorithm").orElse("hmac-md5");
    }
}
