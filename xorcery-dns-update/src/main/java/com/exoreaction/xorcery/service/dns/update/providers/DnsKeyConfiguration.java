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
package com.exoreaction.xorcery.service.dns.update.providers;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record DnsKeyConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseThrow(() -> new IllegalArgumentException("Name missing"));
    }

    public String getSecretName() {
        return configuration.getString("secret").orElseThrow(() -> new IllegalArgumentException("Secret missing"));
    }

    public String getAlgorithm() {
        return configuration.getString("algorithm").orElse("HMAC-MD5.SIG-ALG.REG.INT.");
    }
}
