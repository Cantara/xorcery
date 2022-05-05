package com.exoreaction.reactiveservices.service.greeter;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvent;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.greeter.commands.UpdateGreeting;
import com.exoreaction.reactiveservices.service.greeter.domainevents.UpdatedGreeting;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;
import org.neo4j.internal.helpers.collection.MapUtil;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents.events;

@Singleton
@Contract
public class GreeterApplication {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return "greeter";
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.version("1.0.0").api("greeter", "api/greeter");
        }

        @Override
        protected void configure() {
            context.register(GreeterApplication.class, GreeterApplication.class);
        }
    }

    private DomainEventPublisher domainEventPublisher;
    private GraphDatabase graphDatabase;

    @Inject
    public GreeterApplication(DomainEventPublisher domainEventPublisher,
                              GraphDatabase graphDatabase) {
        this.domainEventPublisher = domainEventPublisher;
        this.graphDatabase = graphDatabase;
    }

    // Reads
    public CompletionStage<String> get(String name) {

        return graphDatabase.execute("MATCH (greeter:Greeter {id:$id}) RETURN greeter.greeting as greeting",
                        new MapUtil.MapBuilder<String, Object>().entry("id", "greeter").create())
                .thenApply(r ->
                {
                    try (GraphResult result = r) {
                        return result.getResult().stream().findFirst().map(m -> m.get("greeting").toString()).orElse("Hello World");
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    // Writes
    public CompletionStage<Metadata> handle(Record command) {
        Metadata metadata = new Metadata().add("app", "Greeter");

        try {
            DomainEvents domainEvents = (DomainEvents) getClass().getDeclaredMethod("handle", command.getClass()).invoke(this, command);
            return domainEventPublisher.publish(metadata, domainEvents);
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private DomainEvents handle(UpdateGreeting updateGreeting) {
        return events(new UpdatedGreeting(updateGreeting.newGreeting()));
    }
}
