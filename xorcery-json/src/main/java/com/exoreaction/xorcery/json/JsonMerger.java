/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * <p>
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
        return merge0(current, adding, true);
    }

    public ObjectNode merge0(ObjectNode current, ObjectNode adding, boolean isRoot) {
        Iterator<Map.Entry<String, JsonNode>> fields = adding.fields();
        fields:
        while (fields.hasNext()) {
            JsonNode currentNode = current;
            Map.Entry<String, JsonNode> entry = fields.next();

            String key;
            if (entry.getKey().startsWith(".")) {
                key = entry.getKey().substring(1);
            } else if (!isRoot) {
                key = entry.getKey();
            } else {
                String[] keys = entry.getKey().split("\\.");
                for (int i = 0; i < keys.length - 1; i++) {
                    JsonNode nextNode = currentNode.path(keys[i]);
                    if (nextNode.isMissingNode()) {
                        nextNode = current.objectNode();
                        ((ObjectNode) currentNode).set(keys[i], nextNode);
                    }
                    currentNode = nextNode;
                }
                key = keys[keys.length - 1];
            }

            if (!currentNode.isObject())
                continue;

            ObjectNode currentObject = (ObjectNode) currentNode;
            setValue(currentObject, key, entry.getValue());
        }
        return current;
    }

    private void setValue(ObjectNode currentObject, String key, JsonNode value) {
        if (value instanceof ObjectNode addingObject) {
            JsonNode currentValue = currentObject.path(key);
            if (currentValue instanceof MissingNode) {
                currentObject.set(key, value);
            } else if (currentValue instanceof ObjectNode currentValueObject) {
                currentObject.set(key, merge0(currentValueObject, addingObject, false));
            }
        } else if (value instanceof ArrayNode addingarray) {
            JsonNode currentValue = currentObject.path(key);
            if (currentValue instanceof MissingNode) {
                currentObject.set(key, addingarray);
            } else if (currentValue instanceof ArrayNode currentArray) {
                currentArray.addAll(addingarray);
            }
        } else {
            currentObject.set(key, value);
        }
    }
}
