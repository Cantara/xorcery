package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.greeter.GreeterApplication;
import com.exoreaction.reactiveservices.service.greeter.commands.UpdateGreeting;
import com.github.jknack.handlebars.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.apache.logging.log4j.LogManager;

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
    public CompletionStage<Context> get() {
        return application.get("greeting").handle((g, t)->
        {
            if (t != null)
            {
                LogManager.getLogger(getClass()).error("Could not get greeting", t);
                return "";
            } else
            {
                return g;
            }
        }).thenApply(Context::newContext);
    }

    @POST
    public CompletionStage<Context> post(@FormParam("greeting") String greetingString) {
        return application.handle(new UpdateGreeting(greetingString))
                .thenCompose(md -> get());
    }
}
