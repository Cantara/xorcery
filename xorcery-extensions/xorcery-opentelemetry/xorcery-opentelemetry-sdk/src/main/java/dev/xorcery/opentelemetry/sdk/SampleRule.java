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
package dev.xorcery.opentelemetry.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.json.JsonElement;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.LinkData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public record SampleRule(Predicate<String> name, SpanKind spanKind,
                         List<Map.Entry<AttributeKey<?>, Predicate<Object>>> attributes) {

    public static SampleRule create(ObjectNode json) {
        JsonElement jsonElement = () -> json;
        List<Map.Entry<AttributeKey<?>, Predicate<Object>>> attributes = jsonElement
                .getObjectAs("attributes", SampleRule::toAttributes)
                .map(attr -> attr.entrySet().stream().toList())
                .orElse(null);
        Predicate<String> namePredicate = null;
        String name = jsonElement.getString("name").orElse(null);
        if (name != null) {
            if (name.indexOf('*') != -1 || name.indexOf('+') != -1 || name.indexOf('?') != -1) {
                Pattern regexp = Pattern.compile(name);
                namePredicate = s -> regexp.matcher(s).matches();
            } else {
                namePredicate = s -> s.equals(name);
            }
        }
        SpanKind spanKind = jsonElement.getEnum("spanKind", SpanKind.class).orElse(null);
        return new SampleRule(namePredicate, spanKind, attributes);
    }

    private static Map<AttributeKey<?>, Predicate<Object>> toAttributes(ObjectNode objectNode) {

        Map<AttributeKey<?>, Predicate<Object>> attributes = new HashMap<>();
        for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
            switch (property.getValue().getNodeType()) {
                case BOOLEAN -> {
                    Boolean value = property.getValue().asBoolean();
                    attributes.put(AttributeKey.booleanKey(property.getKey()), b -> b.equals(value));
                }
                case NUMBER -> {
                    if (property.getValue().isFloatingPointNumber()) {
                        Double value = property.getValue().asDouble();
                        attributes.put(AttributeKey.doubleKey(property.getKey()), d -> d.equals(value));
                    } else {
                        Long value = property.getValue().asLong();
                        attributes.put(AttributeKey.longKey(property.getKey()), l -> l.equals(value));
                    }
                }
                case STRING -> {
                    String value = property.getValue().asText();
                    // Try to guess if this is a regexp
                    if (value.indexOf('*') != -1 || value.indexOf('+') != -1 || value.indexOf('?') != -1) {
                        Pattern regexp = Pattern.compile(value);
                        attributes.put(AttributeKey.stringKey(property.getKey()), s -> regexp.matcher(s.toString()).matches());
                    } else {
                        attributes.put(AttributeKey.stringKey(property.getKey()), s -> s.equals(value));
                    }
                }
            }
        }
        return attributes;
    }

    public boolean matches(String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {

        if (this.name != null) {
            if (!this.name.test(name))
                return false;
        }
        if (this.spanKind != null) {
            if (!spanKind.equals(this.spanKind))
                return false;
        }
        if (this.attributes != null) {
            for (int i = 0; i < this.attributes.size(); i++) {
                Map.Entry<AttributeKey<?>, Predicate<Object>> entry = this.attributes.get(i);
                Object value = attributes.get(entry.getKey());
                if (value != null) {
                    if (!entry.getValue().test(value))
                        return false;
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
