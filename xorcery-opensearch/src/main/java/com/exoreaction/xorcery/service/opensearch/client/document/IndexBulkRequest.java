package com.exoreaction.xorcery.service.opensearch.client.document;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record IndexBulkRequest(List<ObjectNode> requests) {

    public record Builder(List<ObjectNode> requests) {
        public Builder() {
            this(new ArrayList<>());
        }

        public Builder create(String id, ObjectNode json) {
            id = id == null ? UUID.randomUUID().toString() : id;
            requests.add(JsonNodeFactory.instance.objectNode().set("create", JsonNodeFactory.instance.objectNode().set("_id", JsonNodeFactory.instance.textNode(id))));
            requests.add(json);
            return this;
        }

        public IndexBulkRequest build() {
            return new IndexBulkRequest(requests);
        }
    }
}
