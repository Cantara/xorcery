package com.exoreaction.xorcery.service.neo4jprojections;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

import static com.exoreaction.xorcery.service.neo4j.client.Cypher.toField;

public record ProjectionModel(ObjectNode json)
        implements JsonElement {
    public String getId() {
        return getString(toField(Projection.id)).orElseThrow();
    }

    public Optional<Long> getVersion() {
        return getLong(toField(Projection.version));
    }

    public Optional<Long> getRevision() {
        return getLong(toField(Projection.revision));
    }
}
