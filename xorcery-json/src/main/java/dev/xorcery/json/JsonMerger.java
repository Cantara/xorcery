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
package dev.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Given a JSON tree, merge in another JSON tree into it and return the new tree.
 * <p>
 * Primitive values get overwritten by adding node, object values get current added with merged object,
 * and arrays get merged elements added to original array
 */
public class JsonMerger
        implements BiFunction<ObjectNode, ObjectNode, ObjectNode> {

    private final Predicate<Stack<String>> mergeArrayItemsPredicate;

    public JsonMerger() {
        this(stack -> true);
    }

    public JsonMerger(Predicate<Stack<String>> mergeArrayItemsPredicate) {
        this.mergeArrayItemsPredicate = mergeArrayItemsPredicate;
    }


    @Override
    public ObjectNode apply(ObjectNode current, ObjectNode adding) {
        ObjectNode merged = current.deepCopy();
        return merge(merged, adding);
    }

    public ObjectNode merge(ObjectNode current, ObjectNode adding) {
        return merge0(current, adding, true, new Stack<>());
    }

    public ObjectNode merge0(ObjectNode current, ObjectNode adding, boolean isRoot, Stack<String> path) {
        Iterator<Map.Entry<String, JsonNode>> fields = adding.fields();
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
                        if (currentNode instanceof ObjectNode on) {
                            on.set(keys[i], nextNode);
                        }
                    }
                    currentNode = nextNode;
                }
                key = keys[keys.length - 1];
            }

            if (!currentNode.isObject())
                continue;

            if (currentNode instanceof ObjectNode currentObject) {
                setValue(currentObject, key, entry.getValue(), path);
            }
        }
        return current;
    }

    private void setValue(ObjectNode currentObject, String key, JsonNode value, Stack<String> path) {
        path.push(key);
        try {
        if (value instanceof ObjectNode addingObject) {
            JsonNode currentValue = currentObject.path(key);
            if (currentValue instanceof MissingNode) {
                currentObject.set(key, value);
            } else if (currentValue instanceof ObjectNode currentValueObject) {
                currentObject.set(key, merge0(currentValueObject, addingObject, false, path));
            }
        } else if (value instanceof ArrayNode addingarray) {
            JsonNode currentValue = currentObject.path(key);
            if (currentValue.isMissingNode()) {
                currentObject.set(key, addingarray);
            } else if (currentValue instanceof ArrayNode currentArray) {
                Set<JsonNode> updatedObjects = new HashSet<>();
                addingarray.forEach(addingValue ->
                {
                    if (addingValue instanceof ObjectNode addingObject) {
                        boolean updatedObject = false;
                        if (mergeArrayItemsPredicate.test(path))
                        {
                            // Check if object with this identity exists in array
                            for (JsonNode jsonNode : currentArray) {
                                if (jsonNode instanceof ObjectNode currentArrayObject) {
                                    // Merge it
                                    Map.Entry<String, JsonNode> currentIdField = currentArrayObject.fields().next();
                                    Map.Entry<String, JsonNode> addingIdField = addingObject.fields().next();
                                    if (currentIdField.getKey().equals(addingIdField.getKey())
                                            && currentIdField.getValue().equals(addingIdField.getValue())
                                            && !updatedObjects.contains(currentArrayObject)) {
                                        merge0(currentArrayObject, addingObject, false, path);
                                        updatedObject = true;
                                        updatedObjects.add(currentArrayObject);
                                        break;
                                    }
                                }
                            }
                        }
                        if (!updatedObject) {
                            currentArray.add(addingValue);
                            updatedObjects.add(addingValue);
                        }
                    } else {
                        for (JsonNode jsonNode : currentArray) {
                            if (jsonNode.equals(addingValue))
                                return;
                        }
                        currentArray.add(addingValue);
                    }
                });
            } else if (currentValue instanceof TextNode ||
                    currentValue instanceof NumericNode ||
                    currentValue instanceof BooleanNode) {
                ArrayNode arrayCopy = addingarray.deepCopy();
                boolean alreadyInArray = false;
                for (JsonNode jsonNode : arrayCopy) {
                    if (jsonNode.equals(currentValue)) {
                        alreadyInArray = true;
                        break;
                    }
                }
                if (!alreadyInArray) {
                    arrayCopy.add(currentValue);
                }
                currentObject.set(key, arrayCopy);
            }
        } else {

            if (value.asText().startsWith("{{") && value.asText().endsWith("}}")) {
                // This value is a reference, don't try to merge, just replace it
                currentObject.set(key, value);
                return;
            }

            JsonNode currentValue = currentObject.path(key);
            if (currentValue.isMissingNode()) {
                currentObject.set(key, value);
            } else if (currentValue instanceof ArrayNode currentArray) {
                if (value instanceof ObjectNode addingObject) {
                    // Check if object with this identity exists in array
                    boolean updatedObject = false;
                    for (JsonNode jsonNode : currentArray) {
                        if (jsonNode instanceof ObjectNode currentArrayObject) {
                            // Merge it
                            Map.Entry<String, JsonNode> currentIdField = currentArrayObject.fields().next();
                            Map.Entry<String, JsonNode> addingIdField = addingObject.fields().next();
                            if (currentIdField.getKey().equals(addingIdField.getKey()) && currentIdField.getValue().equals(addingIdField.getValue())) {
                                merge0(currentArrayObject, addingObject, false, path);
                                updatedObject = true;
                                break;
                            }
                        }
                    }
                    if (!updatedObject) {
                        currentArray.add(value);
                    }
                } else {
                    for (JsonNode jsonNode : currentArray) {
                        if (jsonNode.equals(value))
                            return;
                    }
                    currentArray.add(value);
                }
            } else {
                currentObject.set(key, value);
            }
        }
        } finally {
            path.pop();
        }
    }
}
