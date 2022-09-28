package com.exoreaction.xorcery.service.opensearch.client.search;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SearchRequest(ObjectNode json) {

    public record Builder(ObjectNode request) {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder query(ObjectNode json) {
            request.set("query", json);
            return this;
        }

        public Builder size(int value) {
            request.set("size", request.numberNode(value));
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(request);
        }
    }
}
