package com.exoreaction.xorcery.jetty.server.security.jwt;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record IssuerConfiguration(Configuration configuration) {

    public Optional<String> getJWKS() {
        return configuration.getString("jwks");
    }

    public List<JwtKey> getKeys() {
        return configuration.getObjectListAs("keys", on -> new JwtKey(new Configuration(on)))
                .orElse(Collections.emptyList());
    }

    public record JwtKey(Configuration configuration) {
        public String getAlg()
        {
            return configuration.getString("alg").orElseThrow(missing("alg"));
        }

        public String getKey()
        {
            return configuration.getString("key").orElseThrow(missing("key"));
        }

        public Optional<String> getKid()
        {
            return configuration.getString("kid");
        }
    }
}
