package com.exoreaction.xorcery.jwt.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record JwtServerConfiguration(Configuration configuration) {

    public String getTokenIssuer() {
        return configuration.getString("token.issuer").orElseThrow(Configuration.missing("issuer"));
    }

    public Duration getTokenDuration() {
        return configuration.getString("token.duration").map(Duration::parse).orElseThrow(Configuration.missing("token.duration"));
    }

    public String getCookieName() {
        return configuration.getString("cookie.name").orElseThrow(Configuration.missing("cookie.name"));
    }

    public String getCookiePath() {
        return configuration.getString("cookie.path").orElseThrow(Configuration.missing("cookie.path"));
    }

    public Duration getCookieDuration() {
        return configuration.getString("cookie.duration").map(Duration::parse).orElseThrow(Configuration.missing("cookie.duration"));
    }

    public String getCookieDomain() {
        return configuration.getString("cookie.domain").orElse(null);
    }

    public List<JwtKey> getKeys() {
        return configuration.getObjectListAs("keys", JwtKey::new).orElse(Collections.emptyList());
    }

    public record JwtKey(Configuration configuration) {
        public JwtKey(ObjectNode on) {
            this(new Configuration(on));
        }

        public String getAlg() {
            return configuration.getString("alg").orElse("ES256");
        }

        public String getKeyId() {
            return configuration.getString("kid").orElseThrow(missing("kid"));
        }

        public String getPublicKey() {
            return configuration.getString("publicKey").orElseThrow(missing("publicKey"));
        }

        public String getPrivateKey() {
            return configuration.getString("privateKey").orElseThrow(missing("privateKey"));
        }
    }
}
