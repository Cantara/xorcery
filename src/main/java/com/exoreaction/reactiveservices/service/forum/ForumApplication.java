package com.exoreaction.reactiveservices.service.forum;

import com.exoreaction.reactiveservices.cqrs.aggregate.Aggregate;
import com.exoreaction.reactiveservices.cqrs.aggregate.AggregateSnapshot;
import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.aggregate.DomainEvents;
import com.exoreaction.reactiveservices.cqrs.context.DomainContext;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.cqrs.metadata.DomainEventMetadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.forum.contexts.CommentContext;
import com.exoreaction.reactiveservices.service.forum.contexts.PostCommentsContext;
import com.exoreaction.reactiveservices.service.forum.contexts.PostContext;
import com.exoreaction.reactiveservices.service.forum.contexts.PostsContext;
import com.exoreaction.reactiveservices.service.forum.model.CommentModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4jdomainevents.aggregates.AggregateSnapshotLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    private final AggregateSnapshotLoader snapshotLoader;
    private GraphDatabase database;

    @Inject
    public ForumApplication(DomainEventPublisher domainEventPublisher, GraphDatabase database) {
        this.domainEventPublisher = domainEventPublisher;
        this.database = database;
        this.snapshotLoader = new AggregateSnapshotLoader(database);
    }

    public PostsContext posts() {
        return new PostsContext(this);
    }

    public PostContext post(PostModel postModel) {
        return new PostContext(this, postModel);
    }

    public CommentContext comment(CommentModel model) {
        return new CommentContext(this, model);
    }

    public PostCommentsContext postComments(PostModel postModel) {
        return new PostCommentsContext(this, postModel);
    }

    public <T extends AggregateSnapshot> CompletionStage<Metadata> handle(Aggregate<T> aggregate, Metadata metadata, Command command) {

        try {
            DomainEventMetadata domainMetadata = new DomainEventMetadata.Builder(metadata.toBuilder())
                    .domain("forum")
                    .aggregateType(aggregate.getClass())
                    .commandType(command.getClass())
                    .build();

            T snapshot;

            if (Command.isCreate(command.getClass())) {
                // Should fail
                try {
                    snapshotLoader.load(domainMetadata, aggregate);
                    return CompletableFuture.failedStage(new BadRequestException("Entity already exists"));
                } catch (Exception e) {
                    // Good!
                    snapshot = aggregate.getSnapshot();
                }

            } else {
                snapshot = snapshotLoader.load(domainMetadata, aggregate);
            }

            DomainEvents events = aggregate.handle(domainMetadata.metadata(), snapshot, command);

            return domainEventPublisher.publish(metadata, events);
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }
}
