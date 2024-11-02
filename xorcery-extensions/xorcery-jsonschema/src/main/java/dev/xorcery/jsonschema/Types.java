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
package dev.xorcery.jsonschema;

/**
 * @author rickardoberg
 */
public enum Types {
    Null,
    Boolean,
    Object,
    Array,
    Number,
    Integer,
    String;

    public static Types typeOf(Class<?> type) {
        return switch (type.getName()) {
            case "long", "java.lang.Long", "int", "java.lang.Integer" -> Types.Integer;
            case "double", "java.lang.Double", "float", "java.lang.Float" -> Types.Number;
            case "boolean", "java.lang.Boolean" -> Types.Boolean;
            case "java.util.List", "java.util.Set", "com.fasterxml.jackson.databind.node.ArrayNode" -> Types.Array;
            case "java.util.Map", "com.fasterxml.jackson.databind.node.ObjectNode", "dev.xorcery.jsonapi.ResourceObject" -> Types.Object; // TODO Invisible dependency here, could break later without us knowing about it
            case "java.time.Period", "java.lang.String" -> Types.String;
            default -> Types.String;
        };
    }
}
