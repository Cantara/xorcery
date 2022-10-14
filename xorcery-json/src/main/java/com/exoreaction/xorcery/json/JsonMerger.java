package com.exoreaction.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Given a JSON tree, merge in another JSON tree into it and return the new tree.
 *
 * Primitive values get overwritten by adding node, object values get current added with merged object,
 * and arrays get merged elements added to original array
 */
public class JsonMerger
        implements BiFunction<ObjectNode, ObjectNode, ObjectNode> {
    @Override
    public ObjectNode apply(ObjectNode current, ObjectNode adding) {
        ObjectNode merged = current.deepCopy();
        return merge(merged, adding);
    }

    public ObjectNode merge(ObjectNode current, ObjectNode adding) {
        Iterator<Map.Entry<String, JsonNode>> fields = adding.fields();
        while (fields.hasNext()) {
            JsonNode currentNode = current;
            Map.Entry<String, JsonNode> entry = fields.next();

            String[] keys = entry.getKey().split("\\.");
            for (int i = 0; i < keys.length - 1; i++) {
                JsonNode nextNode = currentNode.path(keys[i]);
                if (nextNode.isMissingNode())
                {
                    nextNode = current.objectNode();
                    ((ObjectNode) currentNode).set(keys[i], nextNode);
                }
                currentNode = nextNode;
            }
            String key = keys[keys.length-1];

            if (!currentNode.isObject())
                continue;

            ObjectNode currentObject = (ObjectNode)currentNode;

            if (entry.getValue() instanceof ObjectNode addingObject) {
                JsonNode currentValue = currentObject.path(key);
                if (currentValue instanceof MissingNode) {
                    currentObject.set(key, entry.getValue());
                } else if (currentValue instanceof ObjectNode currentValueObject) {
                    currentObject.set(key, apply(currentValueObject, addingObject));
                }
            } else if (entry.getValue() instanceof ArrayNode addingarray) {
                JsonNode currentValue = currentObject.path(key);
                if (currentValue instanceof MissingNode) {
                    currentObject.set(key, addingarray);
                } else if (currentValue instanceof ArrayNode currentArray) {
                    currentArray.addAll(addingarray);
                }
            } else {
                currentObject.set(key, entry.getValue());
            }

        }
        return current;
    }
}
