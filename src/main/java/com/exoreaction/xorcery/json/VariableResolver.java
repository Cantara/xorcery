package com.exoreaction.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve variables in JSON values.
 *
 * Includes support for hierarchical lookups, recursive lookups, and default values.
 *
 * Syntax for strings with variables:
 * some {{ other.variable.name }} value
 * If the JSON source has a path other.variable.name that resolves to "foo" then the resulting value
 * is: "some foo value"
 *
 * Default values are expressed with |, like so:
 * some {{ other.variable.name | bar }} value
 * If the JSON source tree does not have a path other.variable.name then the resulting value is: "some bar value"
 */
public class VariableResolver
        implements BiFunction<ObjectNode, ObjectNode, ObjectNode> {
    /**
     * Resolve variables in root object values from source object. These may or may not be the same.
     *
     * @param source the first function argument
     * @param root   the second function argument
     * @return
     */
    @Override
    public ObjectNode apply(ObjectNode source, ObjectNode root) {
        return resolveObject(source, root);
    }

    private ObjectNode resolveObject(ObjectNode source, ObjectNode node) {
        ObjectNode result = node.objectNode();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String next = names.next();
            JsonNode value = node.get(next);
            if (value instanceof TextNode textNode) {
                try {
                    JsonNode newValue = resolveValue(source, textNode);
                    result.set(next, newValue);
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Could not resolve variables for " + next + ":" + textNode.textValue(), e);
                }
            } else if (value instanceof ObjectNode object) {
                result.set(next, resolveObject(source, object));
            } else if (value instanceof ArrayNode array) {
                result.set(next, resolveArray(source, array));
            } else {
                result.set(next, value);
            }
        }
        return result;
    }

    private ArrayNode resolveArray(ObjectNode source, ArrayNode node) {
        ArrayNode result = node.arrayNode();
        for (JsonNode value : node) {
            if (value instanceof TextNode textNode) {
                try {
                    JsonNode newValue = resolveValue(source, textNode);
                    result.add(newValue);
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Could not resolve variables: " + textNode.textValue(), e);
                }
            } else if (value instanceof ObjectNode object) {
                result.add(resolveObject(source, object));
            } else if (value instanceof ArrayNode array) {
                result.add(resolveArray(source, array));
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private JsonNode resolveValue(ObjectNode source, TextNode value) {
        String textValue = value.textValue();
        String newValue = "";
        Matcher matcher = Pattern.compile("\\{\\{([^}]+)}}").matcher(textValue);
        int idx = 0;
        while (matcher.find()) {
            String subName = matcher.group(1);
            String[] subNameWithDefault = subName.split("\\|");
            JsonNode replacedValue = lookup(source, subNameWithDefault[0].trim()).orElseGet(() ->
            {
                if (subNameWithDefault.length > 1) {
                    for (int i = 1; i < subNameWithDefault.length; i++) {
                        String defaultKey = subNameWithDefault[i];
                        JsonNode defaultValue;
                        try {
                            defaultValue = new ObjectMapper().readTree(new StringReader(defaultKey.trim()));
                            if (defaultValue instanceof TextNode textNode)
                            {
                                JsonNode resolvedValue = resolveValue(source, textNode);
                                if (!(resolvedValue instanceof MissingNode))
                                    defaultValue = resolvedValue;
                            }
                        } catch (IOException e) {
                            defaultValue =  resolveValue(source, JsonNodeFactory.instance.textNode("{{"+defaultKey.trim()+"}}"));
                        }
                        if (!(defaultValue instanceof MissingNode))
                            return defaultValue;
                    }
                    return MissingNode.getInstance();
                } else {
                    return MissingNode.getInstance();
                }
            });

            if (textValue.length() == subName.length() + 4) {
                if (replacedValue instanceof TextNode replacedText)
                    return resolveValue(source, replacedText);
                else if (replacedValue instanceof ObjectNode replacedObject)
                    return resolveObject(source, replacedObject);
                else
                    return replacedValue;
            } else {
                // Resolve again
                if (replacedValue instanceof TextNode replacedText)
                    replacedValue = resolveValue(source, replacedText);

                newValue += textValue.substring(idx, matcher.start()) + replacedValue.asText();
                idx = matcher.end();
            }
        }
        newValue += textValue.substring(idx);
        if (!newValue.equals(textValue))
            return resolveValue(source, JsonNodeFactory.instance.textNode(newValue));
        else
            return value;
    }

    private Optional<JsonNode> lookup(ObjectNode source, String name) {
        ObjectNode c = source;
        String[] names = name.split("\\.");
        for (int i = 0; i < names.length - 1; i++) {
            JsonNode node = c.get(names[i]);
            if (node instanceof ObjectNode object)
                c = resolveObject(source, object);
            else
                return Optional.empty();
        }
        return Optional.ofNullable(c.get(names[names.length - 1]));
    }
}
