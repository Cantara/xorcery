package dev.xorcery.jsonschema.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.xorcery.json.JsonMerger;
import dev.xorcery.jsonschema.JsonSchema;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

public class SchemaMerger {

    public JsonSchema merge(JsonSchema existingSchema, JsonSchema generatedSchema, boolean mergeArrayObjectItems) {
        ObjectNode existingSchemaJson = existingSchema.json().deepCopy();
        ObjectNode generatedSchemaJson = generatedSchema.json().deepCopy();

        // Replace $refs in generatedSchema
        replaceRefs(existingSchemaJson, existingSchemaJson, generatedSchemaJson, generatedSchemaJson);

        // Remove properties in existing schema that are not in the generated schema
        pruneExistingSchema(existingSchemaJson, generatedSchemaJson);

        // Merge existing schema into generated schema
        Set<String> exceptions = Set.of("anyOf", "allOf", "oneOf");
        Predicate<Stack<String>> predicate = mergeArrayObjectItems ? path -> true : path -> !exceptions.contains(path.peek());
        ObjectNode mergedSchemaJson = new JsonMerger(predicate).merge(generatedSchemaJson, existingSchemaJson);
        return new JsonSchema(mergedSchemaJson);
    }

    private void replaceRefs(ObjectNode existingSchemaJson, ObjectNode rootExistingSchemaJson, ObjectNode generatedSchemaJson, ObjectNode rootGeneratedSchemaJson) {

        for (Map.Entry<String, JsonNode> generatedProperty : new HashSet<>(generatedSchemaJson.properties())) {
            JsonNode existingPropertyValue = existingSchemaJson.get(generatedProperty.getKey());
            if (existingPropertyValue instanceof ObjectNode refNode
                    && refNode.get("$ref") instanceof TextNode refValue
                    && refValue.textValue().startsWith("#/$defs/")
                    && !(generatedProperty.getValue() instanceof ObjectNode generatedRefNode
                    && generatedRefNode.get("$ref") instanceof TextNode generatedRefValue
                    && generatedRefValue.textValue().equals(refValue.textValue()))) {
                ObjectNode generatedDefsNode = rootGeneratedSchemaJson.get("$defs") instanceof ObjectNode defsNode
                        ? defsNode
                        : (ObjectNode) rootGeneratedSchemaJson.set("$defs", JsonNodeFactory.instance.objectNode()).get("$defs");
                ObjectNode existingDefsNode = (ObjectNode) rootExistingSchemaJson.get("$defs");

                String refString = refValue.textValue();
                String[] refStrings = refString.substring("#/$defs/".length()).split("/");
                for (int i = 0; i < refStrings.length; i++) {
                    String path = refStrings[i];
                    if (i < refStrings.length - 1) {
                        generatedDefsNode = generatedDefsNode.get(path) instanceof ObjectNode pathNode
                                ? pathNode
                                : (ObjectNode) generatedDefsNode.set(path, JsonNodeFactory.instance.objectNode()).get(path);
                        existingDefsNode = (ObjectNode) existingDefsNode.get(path);
                    } else {
                        generatedDefsNode.set(path, generatedProperty.getValue());
                        generatedSchemaJson.set(generatedProperty.getKey(), existingPropertyValue);
                    }
                }
            } else if (generatedProperty.getValue() instanceof ObjectNode generatedValue && existingPropertyValue instanceof ObjectNode existingValue) {
                replaceRefs(existingValue, rootExistingSchemaJson, generatedValue, rootGeneratedSchemaJson);
            } else if (generatedProperty.getValue() instanceof ArrayNode generatedValue && existingPropertyValue instanceof ArrayNode existingValue) {
                for (int i = 0; i < generatedValue.size(); i++) {
                    JsonNode generatedItem = generatedValue.get(i);
                    JsonNode existingItem = existingValue.get(i);
                    if (generatedItem instanceof ObjectNode generatedItemObject && existingItem instanceof ObjectNode existingItemObject) {
                        replaceRefs(existingItemObject, rootExistingSchemaJson, generatedItemObject, rootGeneratedSchemaJson);
                    }
                }
            }
        }
    }

    private void pruneExistingSchema(ObjectNode existingSchema, ObjectNode generatedSchema) {
        JsonNode generatedSchemaRootProperties = generatedSchema.path("properties");
        for (Map.Entry<String, JsonNode> property : new HashSet<>(existingSchema.path("properties").properties())) {
            if (generatedSchemaRootProperties.get(property.getKey()) instanceof ObjectNode) {
                if (existingSchema.path("$defs").path(property.getKey()) instanceof ObjectNode existingObject) {
                    if (generatedSchema.path("$defs").path(property.getKey()) instanceof ObjectNode generatedObject) {
                        pruneExistingSchema0(existingObject, generatedObject);
                    }
                }
            } else {
                existingSchema.remove(property.getKey());
                if (existingSchema.path("$defs") instanceof ObjectNode defs) {
                    defs.remove(property.getKey());
                }
            }
        }
    }

    // Prune properties from existing schema that are missing in the generated schema
    private void pruneExistingSchema0(ObjectNode existingObject, ObjectNode generatedObject) {
        if (existingObject.path("properties") instanceof ObjectNode existingProperties) {
            for (Map.Entry<String, JsonNode> property : new HashSet<>(existingProperties.properties())) {
                if (generatedObject.path("properties").get(property.getKey()) instanceof ObjectNode propertyNode) {
                    if (propertyNode.has("properties")) {
                        if (property.getValue() instanceof ObjectNode existingChildProperty) {
                            pruneExistingSchema0(existingChildProperty, propertyNode);
                        }
                    }
                } else {
                    existingProperties.remove(property.getKey());
                }
            }
        }
    }
}
