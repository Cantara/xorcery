package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.domainevents.DomainEventsService;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;
import com.exoreaction.reactiveservices.service.greeter.domainevents.GreetedEvent;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.github.jknack.handlebars.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Path("api/greeter")
public class GreeterResource
    extends JsonApiResource
{
    private final DomainEventPublisher eventPublisher;
    private MapDatabaseService mapDatabaseService;

    @Inject
    public GreeterResource(DomainEventsService eventPublisher, MapDatabaseService mapDatabaseService) {
        this.eventPublisher = eventPublisher;
        this.mapDatabaseService = mapDatabaseService;
    }

    @GET
    public Context get(@QueryParam("greeting") String greetingString)
    {
        if (greetingString != null)
        {
            try {
                eventPublisher.publish(new Metadata(), List.of(new GreetedEvent() {{
                    greeting = greetingString;
                }})).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return Context.newContext(Optional.ofNullable(mapDatabaseService.getDatabase().get("greeting")).orElse(""));
    }
}
