package com.exoreaction.reactiveservices.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Given a JSON tree, merge in another JSON tree into it and return the new tree.
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
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getValue() instanceof ObjectNode addingObject) {
                JsonNode currentNode = current.path(entry.getKey());
                if (currentNode instanceof MissingNode) {
                    current.set(entry.getKey(), entry.getValue());
                } else if (currentNode instanceof ObjectNode currentObject) {
                    current.set(entry.getKey(), apply(currentObject, addingObject));
                }
            } else if (entry.getValue() instanceof ArrayNode addingarray) {
                JsonNode currentNode = current.path(entry.getKey());
                if (currentNode instanceof MissingNode) {
                    current.set(entry.getKey(), addingarray);
                } else if (currentNode instanceof ArrayNode currentArray) {
                    currentArray.addAll(addingarray);
                }
            } else {
                current.set(entry.getKey(), entry.getValue());
            }

        }
        return current;
    }
}
