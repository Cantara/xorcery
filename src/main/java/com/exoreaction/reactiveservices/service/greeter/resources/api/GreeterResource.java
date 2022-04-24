package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.domainevents.DomainEventsService;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;
import com.exoreaction.reactiveservices.service.greeter.domainevents.GreetedEvent;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.github.jknack.handlebars.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.concurrent.CompletionStage;

import static com.github.jknack.handlebars.Context.newContext;

@Path("api/greeter")
public class GreeterResource
        extends JsonApiResource {
    private final DomainEventPublisher eventPublisher;
    private final MapDatabaseService mapDatabaseService;

    @Inject
    public GreeterResource(DomainEventsService eventPublisher, MapDatabaseService mapDatabaseService) {
        this.eventPublisher = eventPublisher;
        this.mapDatabaseService = mapDatabaseService;
    }

    @GET
    public Context get() {
        return newContext(mapDatabaseService.get("greeting"));
    }

    @POST
    public CompletionStage<Context> post(@FormParam("greeting") String greetingString) {
        return eventPublisher.publish(new DomainEvents(new Metadata(), new GreetedEvent(greetingString)))
                .thenApply(md -> get());
    }
}
