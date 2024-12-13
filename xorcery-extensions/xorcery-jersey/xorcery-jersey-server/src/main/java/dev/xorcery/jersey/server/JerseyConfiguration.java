package dev.xorcery.jersey.server;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;

import java.util.Map;
import java.util.Optional;

public record JerseyConfiguration(Configuration configuration) {
    public static JerseyConfiguration get(Configuration configuration) {
        return new JerseyConfiguration(configuration.getConfiguration("jersey.server"));
    }

    public Map<String, Object> getProperties()
    {
        return JsonElement.toFlatMap(configuration.getConfiguration("properties").json(), JsonElement::toObject);
    }

    public Optional<Map<String, String>> getMediaTypes() {
        return configuration.getObjectAs("mediaTypes", objectNode -> JsonElement.toMap(objectNode, JsonNode::textValue));
    }
}
