package com.exoreaction.xorcery.jetty.server.security;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record JettySecurityConfiguration(Configuration configuration) {

    public static JettySecurityConfiguration get(Configuration configuration) {
        return new JettySecurityConfiguration(configuration.getConfiguration("jetty.server.security"));
    }

    public Optional<String> getAuthenticationMethod() {
        return configuration.getString("method");
    }

    public List<ConstraintConfiguration> getConstraints() {
        return configuration.getObjectListAs("constraints", on -> new ConstraintConfiguration(new Configuration(on)))
                .orElse(Collections.emptyList());
    }

    public List<MappingConfiguration> getMappings() {
        return configuration.getObjectListAs("mappings", on -> new MappingConfiguration(new Configuration(on)))
                .orElse(Collections.emptyList());
    }


    public record ConstraintConfiguration(Configuration configuration) {
        public String getName() {
            return configuration.getString("name").orElseThrow(Configuration.missing("name"));
        }

        public List<String> getRoles() {
            return configuration.getListAs("roles", JsonNode::textValue)
                    .orElse(Collections.emptyList());
        }

    }

    public record MappingConfiguration(Configuration configuration) {
        public String getPath() {
            return configuration.getString("path").orElseThrow(Configuration.missing("path"));
        }

        public Optional<String> getConstraint() {
            return configuration.getString("constraint");
        }
    }
}
