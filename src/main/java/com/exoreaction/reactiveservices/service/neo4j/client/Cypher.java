package com.exoreaction.reactiveservices.service.neo4j.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Cypher {

    private static Map<Enum<?>,String> defaultMappings = new ConcurrentHashMap<>();
    private static Map<Enum<?>,String> fieldMappings = new ConcurrentHashMap<>();

    public static Map<String, Object> toMap(ObjectNode json )
    {
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        Map<String, Object> parameters = new HashMap<>(json.size());
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();

            parameters.put(next.getKey(), switch (next.getValue().getNodeType()) {
                case ARRAY -> null;
                case OBJECT -> null;
                case STRING -> next.getValue().textValue();
                case NUMBER -> next.getValue().numberValue();
                case BINARY -> null;
                case BOOLEAN -> Boolean.TRUE;
                case MISSING -> null;
                case NULL -> null;
                case POJO -> null;
            });
        }
        return parameters;
    }

    public static Function<Enum<?>,String> defaultMappingReturn()
    {
        return field ->
                defaultMappings.computeIfAbsent( field, f ->
                {
                    // Label enums can be used to return the node as a whole instead of a single property
                    if (f.getDeclaringClass().getSimpleName().equals("Label"))
                    {
                        return f.name() + " as " + f.name();
                    } else
                    {
                        return f.getDeclaringClass().getSimpleName().toLowerCase() + "." + f.name() + " as " +
                                f.getDeclaringClass().getSimpleName().toLowerCase() + "_" + f.name();
                    }
                }
 );
    }

    public static String toField( Enum anEnum )
    {
        return fieldMappings.computeIfAbsent( anEnum, e ->
                e.getDeclaringClass().getSimpleName().toLowerCase() + "_" + e.name() );
    }

}
