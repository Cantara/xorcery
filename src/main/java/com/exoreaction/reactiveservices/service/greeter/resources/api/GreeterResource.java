package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.greeter.GreeterApplication;
import com.exoreaction.reactiveservices.service.greeter.commands.ChangeGreeting;
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
    private GreeterApplication application;

    @Inject
    public GreeterResource(GreeterApplication application) {
        this.application = application;
    }

    @GET
    public Context get() {
        return newContext(application.get("greeting"));
    }

    @POST
    public CompletionStage<Context> post(@FormParam("greeting") String greetingString) {
        return application.handle(new ChangeGreeting(greetingString))
                .thenApply(md -> get());
    }
}
