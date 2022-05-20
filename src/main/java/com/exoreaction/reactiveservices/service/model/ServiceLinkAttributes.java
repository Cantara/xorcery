package com.exoreaction.reactiveservices.service.model;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.HashMap;
import java.util.Map;

public record ServiceLinkAttributes(Attributes attributes) {

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>(attributes.getAttributes().size());
        for (Map.Entry<String, JsonValue> entry : attributes.json().entrySet()) {
            JsonValue value = entry.getValue();
            String mapValue = switch (value.getValueType()) {
                case ARRAY -> null;
                case OBJECT -> null;
                case STRING -> ((JsonString) value).getString();
                case NUMBER -> ((JsonNumber) value).isIntegral() ? Long.toString(((JsonNumber) value).longValue()) :
                        Double.toString(((JsonNumber) value).doubleValue());
                case TRUE -> "true";
                case FALSE -> "false";
                case NULL -> null;
            };
            if (mapValue != null)
                map.put(entry.getKey(), mapValue);

        }
        return map;
    }
}
