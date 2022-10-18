package com.exoreaction.xorcery.service.neo4j.client;

import com.exoreaction.xorcery.util.Enums;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Cypher {

    private static Map<Enum<?>, String> defaultMappings = new ConcurrentHashMap<>();

    public static Map<String, Object> toMap(ObjectNode json) {
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        Map<String, Object> map = new HashMap<>(json.size());
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();

            map.put(next.getKey(), switch (next.getValue().getNodeType()) {
                case ARRAY -> toList((ArrayNode) next.getValue());
                case OBJECT -> toMap((ObjectNode) next.getValue());
                case STRING -> next.getValue().textValue();
                case NUMBER -> next.getValue().numberValue();
                case BINARY -> null;
                case BOOLEAN -> Boolean.TRUE;
                case MISSING -> null;
                case NULL -> null;
                case POJO -> null;
            });
        }
        return map;
    }

    public static List<Object> toList(ArrayNode json) {
        Iterator<JsonNode> fields = json.elements();
        List<Object> list = new ArrayList<>(json.size());
        while (fields.hasNext()) {
            JsonNode next = fields.next();

            list.add(switch (next.getNodeType()) {
                case ARRAY -> toList((ArrayNode) next);
                case OBJECT -> toMap((ObjectNode) next);
                case STRING -> next.textValue();
                case NUMBER -> next.numberValue();
                case BINARY -> null;
                case BOOLEAN -> Boolean.TRUE;
                case MISSING -> null;
                case NULL -> null;
                case POJO -> null;
            });
        }
        return list;
    }

    /**
     * By default maps enums used for query results as:
     * Some.name as some_name
     * <p>
     * Label enums can be used to return the node as a whole instead of a single property
     * E.g. Label.SomeEntity -> SomeEntity as SomeEntity
     *
     * @return
     */
    public static Function<Enum<?>, String> defaultFieldMappings() {
        return field ->
                defaultMappings.computeIfAbsent(field, f ->
                        {
                            if (f.getDeclaringClass().getSimpleName().equals("Label")) {
                                return f.name() + " as " + f.name();
                            } else {
                                return f.getDeclaringClass().getSimpleName() + "." + f.name() + " as " +
                                        f.getDeclaringClass().getSimpleName().toLowerCase() + "_" + f.name();
                            }
                        }
                );
    }

    public static ObjectNode toObjectNode(Map<String, Object> resultRow) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, Object> entry : resultRow.entrySet()) {
            node.set(entry.getKey(), toJsonNode(entry.getValue()));
        }
        return node;
    }

    public static JsonNode toJsonNode(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof String v) {
            return JsonNodeFactory.instance.textNode(v);
        } else if (value instanceof Long v) {
            return JsonNodeFactory.instance.numberNode(v);
        } else if (value instanceof Double v) {
            return JsonNodeFactory.instance.numberNode(v);
        } else if (value instanceof Boolean v) {
            return JsonNodeFactory.instance.booleanNode(v);
        } else if (value instanceof List list) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(list.size());
            for (Object child : list) {
                arrayNode.add(toJsonNode(child));
            }
            return arrayNode;
        } else if (value instanceof Map map) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            map.forEach((k, v)-> objectNode.set(k.toString(), toJsonNode(v)));
            return objectNode;
        } else if (value instanceof Node node) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            for (Map.Entry<String, Object> entry : node.getAllProperties().entrySet()) {
                objectNode.set(entry.getKey(), toJsonNode(entry.getValue()));
            }
            return objectNode;
        } else {
            return NullNode.getInstance();
        }
    }

    public static List<String> getCypherStatements(String statementResourceFile)
            throws IllegalArgumentException {
        try (InputStream resourceAsStream = ClassLoader.getSystemResourceAsStream(statementResourceFile)) {
            if (resourceAsStream == null)
                throw new IllegalArgumentException("No such resource file:" + statementResourceFile);

            return Stream.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8))
                    .flatMap(s -> Stream.of(s.split(";")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load Cypher statements", e);
        }
    }

    public static List<String> getCypherStatements(URL statementResourceFile)
            throws IllegalArgumentException {
        try (InputStream resourceAsStream = statementResourceFile.openStream()) {
            if (resourceAsStream == null)
                throw new IllegalArgumentException("No such resource file:" + statementResourceFile);

            return Stream.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8))
                    .flatMap(s -> Stream.of(s.split(";")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load Cypher statements", e);
        }
    }

}
