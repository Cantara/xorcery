package com.exoreaction.reactiveservices.service.forum;

import com.exoreaction.reactiveservices.cqrs.Aggregate;
import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.DomainEvents;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.forum.contexts.PostContext;
import com.exoreaction.reactiveservices.service.forum.contexts.PostsContext;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;

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

    @Inject
    public ForumApplication(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
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
