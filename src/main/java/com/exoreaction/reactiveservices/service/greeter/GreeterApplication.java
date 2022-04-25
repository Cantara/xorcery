package com.exoreaction.reactiveservices.service.greeter;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Attributes;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;
import com.exoreaction.reactiveservices.service.greeter.commands.ChangeGreeting;
import com.exoreaction.reactiveservices.service.greeter.domainevents.GreetedEvent;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.spi.Contract;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@Contract
public class GreeterApplication {

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server) {

            server.addService(new ResourceObject.Builder("service", "greeter")
                    .attributes(new Attributes.Builder()
                            .attribute("version", "1.0"))
                    .links(new Links.Builder()
                            .link("greeter", server.getBaseUriBuilder().path("api/greeter")))
                    .build());

            context.register(GreeterApplication.class, GreeterApplication.class);
            return super.configure(context, injectionManager, server);
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
