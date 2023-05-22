package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.spi.ConfigurationProvider;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ConfigurationProviderContainerNode
        extends ContainerNode<ConfigurationProviderContainerNode> {

    private final ConfigurationProvider configurationProvider;

    public ConfigurationProviderContainerNode(ConfigurationProvider configurationProvider) {
        super(JsonNodeFactory.instance);
        this.configurationProvider = configurationProvider;
    }

    @Override
    public void forEach(Consumer<? super JsonNode> action) {
        // no traversal allowed
    }

    @Override
    public JsonToken asToken() {
        return JsonToken.VALUE_STRING;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public JsonNode get(int index) {
        return null;
    }

    @Override
    public JsonNode get(String fieldName) {
        return configurationProvider.getJson(fieldName);
    }

    @Override
    protected ObjectNode _withObject(JsonPointer origPtr, JsonPointer currentPtr, OverwriteMode overwriteMode, boolean preferIndex) {
        return null;
    }

    @Override
    public ConfigurationProviderContainerNode removeAll() {
        return this;
    }

    @Override
    public int hashCode() {
        return configurationProvider.hashCode();
    }

    @Override
    public JsonNode path(int index) {
        return null;
    }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        return null;
    }

    @Override
    public ConfigurationProviderContainerNode deepCopy() {
        return this;
    }

    @Override
    public JsonNode path(String fieldName) {
        JsonNode n = configurationProvider.getJson(fieldName);
        if (n != null) {
            return n;
        }
        return MissingNode.getInstance();
    }

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.POJO;
    }

    @Override
    public String asText() {
        return configurationProvider.toString();
    }

    @Override
    public JsonNode findValue(String fieldName) {
        return null;
    }

    @Override
    public ObjectNode findParent(String fieldName) {
        return null;
    }

    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
        return null;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) {
        return null;
    }

    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
        return null;
    }

    @Override
    public String toString() {
        return configurationProvider.toString();
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString("{"+configurationProvider.toString()+"}");
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        gen.writeString("{"+configurationProvider.toString()+"}");
    }
}
