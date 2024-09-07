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
package com.exoreaction.xorcery.jetty.server.security.jwt;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record IssuerConfiguration(Configuration configuration) {

    public String getName()
    {
         return configuration.getString("name").orElseThrow(Configuration.missing("name"));
    }

    public Optional<String> getJWKS() {
        return configuration.getString("jwks");
    }

    public List<JwtKey> getKeys() {
        return configuration.getObjectListAs("keys", on -> new JwtKey(new Configuration(on)))
                .orElse(Collections.emptyList());
    }

    public record JwtKey(Configuration configuration) {
        public Optional<String> getKid()
        {
            return configuration.getString("kid");
        }

        public String getAlg()
        {
            return configuration.getString("alg").orElseThrow(missing("alg"));
        }

        public String getPublicKey()
        {
            return configuration.getString("publicKey").orElseThrow(missing("publicKey"));
        }

    }
}
