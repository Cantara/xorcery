package com.exoreaction.xorcery.service.forum;

import com.exoreaction.xorcery.cqrs.aggregate.Aggregate;
import com.exoreaction.xorcery.cqrs.aggregate.AggregateSnapshot;
import com.exoreaction.xorcery.cqrs.aggregate.Command;
import com.exoreaction.xorcery.cqrs.aggregate.DomainEvents;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.xorcery.service.forum.contexts.CommentContext;
import com.exoreaction.xorcery.service.forum.contexts.PostCommentsContext;
import com.exoreaction.xorcery.service.forum.contexts.PostContext;
import com.exoreaction.xorcery.service.forum.contexts.PostsContext;
import com.exoreaction.xorcery.service.forum.model.CommentModel;
import com.exoreaction.xorcery.service.forum.model.PostModel;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4jprojections.Neo4jProjections;
import com.exoreaction.xorcery.service.neo4jprojections.WaitForProjections;
import com.exoreaction.xorcery.service.neo4jprojections.aggregates.AggregateSnapshotLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.*;

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
    private Neo4jProjections neo4jProjections;
    private final WaitForProjections listener;
    private GraphDatabase database;

    @Inject
    public ForumApplication(DomainEventPublisher domainEventPublisher, GraphDatabase database, Neo4jProjections neo4jProjections) {
        this.domainEventPublisher = domainEventPublisher;
        this.database = database;
        this.snapshotLoader = new AggregateSnapshotLoader(database);
        this.neo4jProjections = neo4jProjections;
        listener = new WaitForProjections();
        neo4jProjections.addProjectionListener(listener);
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

            return domainEventPublisher.publish(metadata, events)
                    .thenCompose(md -> listener.waitFor(md));
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

}
