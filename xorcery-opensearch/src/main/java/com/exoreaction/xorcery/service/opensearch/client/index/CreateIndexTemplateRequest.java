package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CreateIndexTemplateRequest(ObjectNode json)
    implements JsonElement
{
    public record Builder(ObjectNode json)
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public CreateIndexTemplateRequest build()
        {
            return new CreateIndexTemplateRequest(json);
        }
    }
}
