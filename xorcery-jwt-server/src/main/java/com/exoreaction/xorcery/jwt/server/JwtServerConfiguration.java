package com.exoreaction.xorcery.jwt.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record JwtServerConfiguration(Configuration configuration) {

    public String getIssuer()
    {
        return configuration.getString("issuer").orElseThrow(Configuration.missing("issuer"));
    }

    public List<JwtKey> getKeys()
    {
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
            return configuration.getString("keyId").orElseThrow(missing("keyId"));
        }

        public String getPublicKey() {
            return configuration.getString("publicKey").orElseThrow(missing("publicKey"));
        }

        public String getPrivateKey() {
            return configuration.getString("privateKey").orElseThrow(missing("privateKey"));
        }
    }
}
