package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/*
Successful response
{
    "token_type": "Bearer",
    "expires_in": 3599,
    "ext_expires_in": 3599,
    "access_token": "..."
}
 */
public record AzureTokenResponse(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureTokenResponse.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public AzureTokenResponse build() {
            return new AzureTokenResponse(builder);
        }
    }

    public boolean hasError() {
        return object().has("error");
    }

    public AzureTokenError getError() {
        return new AzureTokenError(object());
    }

    public String getTokenType() {
        return object().get("token_type").asText();
    }

    public int getExpiresIn() {
        return object().get("expires_in").asInt();
    }

    public String getAccessToken() {
        return object().get("access_token").asText();
    }
}
