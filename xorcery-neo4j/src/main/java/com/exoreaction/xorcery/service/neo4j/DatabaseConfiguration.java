package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;

public record DatabaseConfiguration(ObjectNode json)
        implements JsonElement {
    String getName() {
        return getString("name").orElseThrow();
    }

    List<String> getStartupCypherStatements() {
        return getListAs("startup", JsonNode::textValue).orElse(Collections.emptyList());
    }
}
