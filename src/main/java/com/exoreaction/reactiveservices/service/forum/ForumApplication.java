package com.exoreaction.reactiveservices.service.forum;

import com.exoreaction.reactiveservices.cqrs.Aggregate;
import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.DomainEvents;
import com.exoreaction.reactiveservices.disruptor.Metadata;
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
import com.exoreaction.reactiveservices.service.forum.resources.api.ApiRelationships;
import com.exoreaction.reactiveservices.service.forum.resources.api.ApiTypes;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Singleton
@Contract
public class ForumApplication {

    public static final String SERVICE_TYPE = "forum";

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
    }

    public ResourceDocumentSchema getSchema(URI absolutePath) {
        ResourceDocumentSchema.Builder builder = new ResourceDocumentSchema.Builder();
        builder.with(selfDescribedByLinks(absolutePath))
                .resources(postSchema(), commentSchema())
                .included(commentSchema())
                .builder()
                .builder()
                .title("Forum application");
        return builder.build();
    }

    private Consumer<ResourceDocumentSchema.Builder> selfDescribedByLinks(URI absolutePath) {
        return builder ->
        {

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
