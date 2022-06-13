package com.exoreaction.reactiveservices.service.forum;

import com.exoreaction.reactiveservices.cqrs.Aggregate;
import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.DomainEvents;
import com.exoreaction.reactiveservices.cqrs.annotations.Update;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.hyperschema.model.Link;
import com.exoreaction.reactiveservices.hyperschema.model.Links;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.schema.ResourceDocumentSchema;
import com.exoreaction.reactiveservices.jsonapi.schema.ResourceObjectSchema;
import com.exoreaction.reactiveservices.jsonapi.schema.annotations.AttributeSchema;
import com.exoreaction.reactiveservices.jsonapi.schema.annotations.Cardinality;
import com.exoreaction.reactiveservices.jsonapi.schema.annotations.RelationshipSchema;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import com.exoreaction.reactiveservices.jsonschema.model.Properties;
import com.exoreaction.reactiveservices.jsonschema.model.Types;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.forum.contexts.PostContext;
import com.exoreaction.reactiveservices.service.forum.contexts.PostsContext;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;
import com.exoreaction.reactiveservices.service.forum.resources.api.ApiRelationships;
import com.exoreaction.reactiveservices.service.forum.resources.api.ApiTypes;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;
import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;
import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.self;

@Singleton
@Contract
public class ForumApplication {

    public static final String SERVICE_TYPE = "forum";
    private final ObjectMapper objectMapper;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder
                    .version("1.0.0")
                    .attribute("domain", "forum")
                    .api("forum", "api/forum");
        }

        @Override
        protected void configure() {
            context.register(ForumApplication.class, ForumApplication.class);
        }
    }

    private final DomainEventPublisher domainEventPublisher;

    @Inject
    public ForumApplication(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
        this.objectMapper = new ObjectMapper();
    }

    public ResourceDocumentSchema getSchema(URI absolutePath) {
        ResourceDocumentSchema.Builder builder = new ResourceDocumentSchema.Builder();
        builder.resources(postSchema(), commentSchema())
                .included(commentSchema())
                .builder()
                .links(new Links.Builder().with(
                                selfDescribedByLinks(absolutePath),
                                commands(PostAggregate.class),
                                l -> l.link(new Link.UriTemplateBuilder("posts")
                                        .parameter("post_fields", "Post fields", "Post fields to include")
                                        .parameter("sort", "Sort", "Post sort field")
                                        .parameter("skip", "Skip", "Nr of posts to skip")
                                        .parameter("limit", "Limit", "Limit nr of posts")
                                        .build()))
                        .build())
                .builder()
                .title("Forum application");
        return builder.build();
    }

    private Consumer<Links.Builder> selfDescribedByLinks(URI absolutePath) {
        return builder ->
        {
            builder.link(Link.Builder.link("Self", "Self link", self, APPLICATION_JSON_API))
                    .link(Link.Builder.link("JSON Schema", "JSON Schema for this resource", describedby, APPLICATION_JSON_API, absolutePath.toASCIIString()));
        };
    }

    private ResourceObjectSchema postSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.post)
                .relationships(relationships(ApiRelationships.Post.values()))
                .attributes(attributes(ForumModel.Post.values()))
                .with(b -> b.builder().builder().title("Post"))
                .build();
    }

    private ResourceObjectSchema commentSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.comment)
                .attributes(attributes(ForumModel.Comment.values()))
                .with(b -> b.builder().builder().title("Comment"))
                .build();
    }

    public Consumer<Properties.Builder> relationships(Enum<?>... relationshipEnums) {
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

    public Consumer<Properties.Builder> attributes(Enum<?>... attrs) {
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

                    attributes.property(Cypher.toField(attr), builder.build());
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public Consumer<Links.Builder> commands(Class<?>... commandClasses) {
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
                    objectMapper.acceptJsonFormatVisitor(commandClass, new JsonFormatVisitorWrapper.Base() {
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

                Link.Builder builder = new Link.Builder()
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

    public PostsContext posts() {
        return new PostsContext(this);
    }

    public PostContext post(PostModel postModel) {
        return new PostContext(this, postModel);
    }

    public CompletionStage<Metadata> handle(Aggregate aggregate, Metadata metadata, Command command) {

        // TODO Load snapshot

        try {
            DomainEvents events = aggregate.handle(metadata, aggregate.getSnapshot(), command);

            metadata = new DomainEventMetadata.Builder(metadata.toBuilder())
                    .domain("forum")
                    .aggregateType(aggregate.getClass())
                    .commandType(command.getClass())
                    .build().metadata();

            return domainEventPublisher.publish(metadata, events);
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }
}
