package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public record AzureDnsRecordError(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<AzureDnsRecordError.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public AzureDnsRecordError build() {
            return new AzureDnsRecordError(builder);
        }
    }
    public String getCode() {
        return object().get("code").asText();
    }

    public boolean hasDetails() {
        return object().has("details");
    }

    public List<AzureDnsRecordError> getDetails() {
        return object().path("details") instanceof ArrayNode array ?
            JsonElement.getValuesAs(array, AzureDnsRecordError::new) :
            JsonElement.getValuesAs(JsonNodeFactory.instance.arrayNode(), AzureDnsRecordError::new);
    }

    public String getMessage() {
        return object().get("message").asText();
    }

    public String getTarget() {
        return object().get("target").asText();
    }
}
