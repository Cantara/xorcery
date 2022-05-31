package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.greeter.GreeterApplication;
import com.github.jknack.handlebars.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
