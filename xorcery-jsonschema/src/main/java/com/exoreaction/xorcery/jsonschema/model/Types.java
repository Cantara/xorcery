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
        // TODO Invisible dependency here, could break later without us knowing about it
        switch (type.getName()) {
            case "long":
            case "java.lang.Long":
            case "int":
            case "java.lang.Integer":
                return Types.Integer;
            case "double":
            case "java.lang.Double":
            case "float":
            case "java.lang.Float":
                return Types.Number;
            case "boolean":
            case "java.lang.Boolean":
                return Types.Boolean;
            case "java.util.List":
            case "java.util.Set":
            case "com.fasterxml.jackson.databind.node.ArrayNode":
                return Types.Array;
            case "java.util.Map":
            case "com.fasterxml.jackson.databind.node.ObjectNode":
            case "com.exoreaction.xorcery.jsonapi.model.ResourceObject":
                return Types.Object;
            case "java.time.Period":
            case "java.lang.String":
                return Types.String;
            default:
                return Types.String;
        }
    }
}
