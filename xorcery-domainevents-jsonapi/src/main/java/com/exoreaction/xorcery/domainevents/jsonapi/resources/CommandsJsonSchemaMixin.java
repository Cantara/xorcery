package com.exoreaction.xorcery.domainevents.jsonapi.resources;

import com.exoreaction.xorcery.domainevents.helpers.entity.Command;
import com.exoreaction.xorcery.domainevents.helpers.entity.annotation.Update;
import com.exoreaction.xorcery.hyperschema.model.Links;
import com.exoreaction.xorcery.jsonapi.server.resources.ResourceContext;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.exoreaction.xorcery.jsonschema.model.Properties;
import com.exoreaction.xorcery.jsonschema.model.Types;
import com.exoreaction.xorcery.jsonschema.server.annotations.AttributeSchema;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.exoreaction.xorcery.jsonapi.MediaTypes.APPLICATION_JSON_API;

public interface CommandsJsonSchemaMixin
        extends ResourceContext
{
    // JSON Schema builders
    default Consumer<Links.Builder> commands(Class<?>... commandClasses) {
        return links ->
        {
            for (Class<?> clazz : commandClasses) {

                // Check if this is an enclosing class
                commands(clazz.getClasses()).accept(links);

                if (!Command.class.isAssignableFrom(clazz))
                    continue;

                Class<? extends Command> commandClass = (Class<? extends Command>) clazz;

                final List<BeanProperty> commandProperties = new ArrayList<>();
                try {
                    objectMapper().acceptJsonFormatVisitor(commandClass, new JsonFormatVisitorWrapper.Base() {
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

                String submissionMediaType = APPLICATION_JSON_API;

                List<String> required = new ArrayList<>();

                Properties.Builder properties = new Properties.Builder();
                for (BeanProperty property : commandProperties) {
/*
                if ( field.getType().equals( BodyPart.class ) )
                {
                    submissionMediaType = MultiPartMediaTypes.MULTIPART_MIXED;
                    continue;
                }
*/
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

                String commandName = Command.getName(commandClass);

                JsonSchema commandSchema = new JsonSchema.Builder()
                        .type(Types.Object)
                        .properties(new Properties.Builder()
                                .property("data", new JsonSchema.Builder()
                                        .type(Types.Object)
                                        .additionalProperties(false)
                                        .required("type", "data")
                                        .properties(new Properties.Builder()
                                                .property("id", new JsonSchema.Builder().type(Types.String).build())
                                                .property("type", new JsonSchema.Builder().constant(JsonNodeFactory.instance.textNode(commandName)).build())
                                                .property("attributes", new JsonSchema.Builder()
                                                        .type(Types.Object)
                                                        .required(required.toArray(new String[0]))
                                                        .properties(properties.build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build();

                com.exoreaction.xorcery.hyperschema.model.Link.Builder builder = new com.exoreaction.xorcery.hyperschema.model.Link.Builder()
                        .rel(commandName)
                        .href("{+command_href}")
                        .templateRequired("command_href")
                        .templatePointer("command_href", "0/data/links/" + commandName)
                        .submissionMediaType(submissionMediaType);

                if (commandClass.isAnnotationPresent(Update.class)) {
                    builder.targetSchema(commandSchema);
                } else {
                    builder.submissionSchema(commandSchema);
                }

                links.link(builder.build());
            }
        };
    }
}
