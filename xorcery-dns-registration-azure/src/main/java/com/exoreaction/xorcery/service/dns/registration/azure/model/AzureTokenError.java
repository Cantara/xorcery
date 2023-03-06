package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * @author kbjerke
 * modeled after the error response in <a href="https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow#error-response-1">Azure OAuth2 code flow</a>
 */
public record AzureTokenError(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureTokenError.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder error(AzureTokenError error) {
            builder.setAll(error.json());
            return this;
        }

        public AzureTokenError build() {
            return new AzureTokenError(builder);
        }
    }

    public String getError() {
        return object().get("error").asText();
    }

    public String getErrorDescription() {
        return object().get("error_description").asText();
    }

    public List<Integer> getErrorCodes() {
        var array = object().path("error_codes") instanceof ArrayNode arrayNode ? arrayNode : JsonNodeFactory.instance.arrayNode();
        return JsonElement.getValuesAs(array(), JsonNode::asInt);
    }

    public String getTimestamp() {
        return object().get("timestamp").asText();
    }

    public String getTraceId() {
        return object().get("trace_id").asText();
    }

    public String getCorrelationId() {
        return object().get("correlation_id").asText();
    }
}
