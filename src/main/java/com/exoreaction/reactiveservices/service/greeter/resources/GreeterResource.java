package com.exoreaction.reactiveservices.service.greeter.resources;

import com.exoreaction.reactiveservices.jaxrs.JsonApiResource;
import com.exoreaction.reactiveservices.service.domainevents.DomainEventsService;
import com.exoreaction.reactiveservices.service.domainevents.spi.Metadata;
import com.exoreaction.reactiveservices.service.greeter.domainevents.GreetedEvent;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Path("api/greeter")
public class GreeterResource
    extends JsonApiResource
{
    private final DomainEventsService domainEventsService;
    private MapDatabaseService mapDatabaseService;

    @Inject
    public GreeterResource(DomainEventsService domainEventsService, MapDatabaseService mapDatabaseService) {
        this.domainEventsService = domainEventsService;
        this.mapDatabaseService = mapDatabaseService;
    }

    @GET
    public String get(@QueryParam("greeting") String greetingString)
    {
        if (greetingString != null)
        {
            try {
                domainEventsService.publish(new Metadata(), List.of(new GreetedEvent() {{
                    greeting = greetingString;
                }})).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return Optional.ofNullable(mapDatabaseService.getDatabase().get("greeting")).orElse("");
    }
}
