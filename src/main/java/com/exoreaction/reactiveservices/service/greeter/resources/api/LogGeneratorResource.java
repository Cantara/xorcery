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
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletionStage;

@Path("api/loggenerator")
public class LogGeneratorResource
        extends JsonApiResource {
    private GreeterApplication application;

    @Inject
    public LogGeneratorResource(GreeterApplication application) {
        this.application = application;
    }

    @GET
    public Context get() {
        Logger logger = LogManager.getLogger(getClass());

        for (int i = 0; i < 10000000; i++) {
            logger.info("Count "+i);
        }

        return Context.newContext("Success");
    }
}
