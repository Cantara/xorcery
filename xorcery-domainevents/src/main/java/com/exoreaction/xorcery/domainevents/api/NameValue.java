package com.exoreaction.xorcery.domainevents.api;

import com.fasterxml.jackson.databind.JsonNode;

public record NameValue(String name, JsonNode value) {
}
