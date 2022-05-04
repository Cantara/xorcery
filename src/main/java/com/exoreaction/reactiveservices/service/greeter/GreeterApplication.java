package com.exoreaction.reactiveservices.service.greeter;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.greeter.commands.ChangeGreeting;
import com.exoreaction.reactiveservices.service.greeter.domainevents.GreetedEvent;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    private MapDatabaseService mapDatabaseService;

    @Inject
    public GreeterApplication(DomainEventPublisher domainEventPublisher,
                              MapDatabaseService mapDatabaseService) {
        this.domainEventPublisher = domainEventPublisher;
        this.mapDatabaseService = mapDatabaseService;
    }

    // Reads
    public String get(String name) {
        return mapDatabaseService.get(name);
    }

    // Writes
    public CompletionStage<Metadata> handle(Record command) {
        Metadata metadata = new Metadata();

        try {
            return (CompletionStage<Metadata>) getClass().getDeclaredMethod("handle", Metadata.class, command.getClass()).invoke(this, metadata, command);
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private CompletionStage<Metadata> handle(Metadata metadata, ChangeGreeting changeGreeting) {
        return domainEventPublisher.publish(new DomainEvents(metadata, new GreetedEvent(changeGreeting.newGreeting())));
    }
}
