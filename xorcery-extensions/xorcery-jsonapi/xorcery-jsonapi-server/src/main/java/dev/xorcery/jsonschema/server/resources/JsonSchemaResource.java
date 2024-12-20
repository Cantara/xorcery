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
package dev.xorcery.jsonschema.server.resources;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import dev.xorcery.hyperschema.Link;
import dev.xorcery.hyperschema.Links;
import dev.xorcery.jaxrs.server.resources.ContextResource;
import dev.xorcery.jsonapischema.ResourceDocumentSchema;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.Properties;
import dev.xorcery.jsonschema.Types;
import dev.xorcery.jsonschema.server.annotations.AttributeSchema;
import dev.xorcery.jsonschema.server.annotations.Cardinality;
import dev.xorcery.jsonschema.server.annotations.RelationshipSchema;
import dev.xorcery.lang.Enums;
import jakarta.ws.rs.core.MediaType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static dev.xorcery.jsonapi.JsonApiRels.describedby;
import static dev.xorcery.jsonapi.JsonApiRels.self;
import static dev.xorcery.jsonapi.MediaTypes.APPLICATION_JSON_API;

public interface JsonSchemaResource
        extends ContextResource {

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    default Link selfLink() {
        return Link.Builder.link("Self", "Self link", self, APPLICATION_JSON_API);
    }

    default Link describedbyLink(String schemaPath) {
        return Link.Builder.link("JSON Schema", "JSON Schema for this resource", describedby, APPLICATION_JSON_API, schemaPath);
    }

    default Consumer<Properties.Builder> relationships(Enum<?>... relationshipEnums) {
        return relationships ->
        {
            for (Enum<?> relationshipEnum : relationshipEnums) {
                try {
                    Field field = relationshipEnum.getClass().getField(relationshipEnum.name());

                    RelationshipSchema schema = field.getAnnotation(RelationshipSchema.class);

                    if (schema != null) {
                        if (schema.cardinality().equals(Cardinality.one)) {
                            relationships.property(relationshipEnum.name(),
                                    new JsonSchema.Builder()
                                            .oneOf(ResourceDocumentSchema.resourceObjectIdentifierRef,
                                                    ResourceDocumentSchema.relationshipRelatedLinksRef)
                                            .build());
                        } else {
                            relationships.property(relationshipEnum.name(), new JsonSchema.Builder()
                                    .type(Types.Array)
                                    .items(ResourceDocumentSchema.resourceObjectIdentifierRef,
                                            ResourceDocumentSchema.relationshipRelatedLinksRef)
                                    .build());
                        }
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    default Consumer<Properties.Builder> attributes(Enum<?>... attrs) {
        return attributes ->
        {
            for (Enum<?> attr : attrs) {
                try {
                    JsonSchema.Builder builder = new JsonSchema.Builder();
                    Field field = attr.getClass().getField(attr.name());

                    AttributeSchema schema = field.getAnnotation(AttributeSchema.class);
                    Types attributeType;

                    if (schema != null) {
                        builder
                                .title(schema.title())
                                .description(schema.description())
                                .type(schema.type());

                        if (!Enum.class.equals(schema.values())) {
                            builder.enums(Arrays
                                    .stream((Enum<?>[]) schema.values().getEnumConstants())
                                    .map(Enum::name)
                                    .toArray(String[]::new));
                        }
                    } else {
                        builder.title(attr.name());
                        builder.type(Types.String);
                    }

                    attributes.property(Enums.toField(attr), builder.build());
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    default Consumer<Links.Builder> websocket(String rel, Class<?> parameterClass) {
        return links ->
        {
            final List<BeanProperty> commandProperties = new ArrayList<>();
            try {
                objectMapper.acceptJsonFormatVisitor(parameterClass, new JsonFormatVisitorWrapper.Base() {
                    @Override
                    public JsonObjectFormatVisitor expectObjectFormat(JavaType type) throws JsonMappingException {
                        return new JsonObjectFormatVisitor.Base() {
                            @Override
                            public void property(BeanProperty prop) throws JsonMappingException {
                                commandProperties.add(prop);
                            }

                            @Override
                            public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException {
                                super.property(name, handler, propertyTypeHint);
                            }

                            @Override
                            public void optionalProperty(BeanProperty prop) throws JsonMappingException {
                                commandProperties.add(prop);
                            }

                            @Override
                            public void optionalProperty(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException {
                                super.optionalProperty(name, handler, propertyTypeHint);
                            }
                        };
                    }
                });
            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            }

            String submissionMediaType = MediaType.APPLICATION_JSON;

            List<String> required = new ArrayList<>();

            Properties.Builder properties = new Properties.Builder();
            for (BeanProperty property : commandProperties) {

                JsonSchema.Builder builder = new JsonSchema.Builder()
                        .type(Types.typeOf(property.getType().getRawClass()))
                        .title(property.getName());

                AttributeSchema schema = property.getAnnotation(AttributeSchema.class);
                if (schema != null) {
                    if (schema.required()) {
                        required.add(property.getName());
                    }

                    builder.title(schema.title())
                            .description(schema.description());
                } else {
                    builder.title(property.getName());
                    required.add(property.getName());
                }

                if (Enum.class.isAssignableFrom(property.getType().getRawClass())) {
                    builder.enums(Arrays
                            .stream((Enum<?>[]) property.getType().getRawClass().getEnumConstants())
                            .map(Enum::name)
                            .toArray(String[]::new));
                }

                properties.property(property.getName(), builder.build());
            }

            JsonSchema websocketParametersSchema = new JsonSchema.Builder()
                    .type(Types.Object)
                    .additionalProperties(false)
                    .required(required.toArray(new String[0]))
                    .properties(properties.build())
                    .build();

            Link.Builder builder = new Link.Builder()
                    .rel(rel)
                    .href("{+websocket_href}")
                    .templateRequired("websocket_href")
                    .templatePointer("websocket_href", "0/data/links/" + rel)
                    .submissionMediaType(submissionMediaType);

            builder.submissionSchema(websocketParametersSchema);

            links.link(builder.build());
        };
    }
}
