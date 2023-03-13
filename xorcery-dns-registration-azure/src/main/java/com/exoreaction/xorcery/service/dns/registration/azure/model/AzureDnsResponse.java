package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public record AzureDnsResponse(ObjectNode json)
        implements JsonElement {

    public boolean hasNext() {
        return object().has("nextLink") && !"".equals(object().get("nextLink").asText());
    }

    public String getNextLink() {
        return object().get("nextLink").asText();
    }

    public List<AzureDnsRecord> list() {
        return object().path("value") instanceof ArrayNode array ?
                JsonElement.getValuesAs(array, AzureDnsRecord::new)
                : JsonElement.getValuesAs(JsonNodeFactory.instance.arrayNode(), AzureDnsRecord::new);
    }
}
