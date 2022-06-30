package com.exoreaction.xorcery.jsonschema.model;

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
            case "java.util.List", "java.util.Set","com.fasterxml.jackson.databind.node.ArrayNode" -> Types.Array;
            case "java.util.Map","com.fasterxml.jackson.databind.node.ObjectNode" -> Types.Object;
            case "java.time.Period", "java.lang.String" -> Types.String;
            default -> Enum.class.isAssignableFrom(type) ? Types.String : null;
        };
    }
}
