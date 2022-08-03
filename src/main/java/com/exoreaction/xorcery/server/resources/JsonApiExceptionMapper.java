package com.exoreaction.xorcery.server.resources;

import com.exoreaction.xorcery.jsonapi.model.Error;
import com.exoreaction.xorcery.jsonapi.model.Errors;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletionException;

import static com.exoreaction.xorcery.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Provider
@Produces(APPLICATION_JSON_API)
public class JsonApiExceptionMapper
    implements ExceptionMapper<Throwable>
{
    Logger logger = LogManager.getLogger(getClass());
    @Override
    public Response toResponse(Throwable exception)
    {
        if (exception instanceof CompletionException)
        {
            exception = exception.getCause();
        }

        try {
            throw exception;
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (Throwable e) {
            logger.error("Response exception", exception);

            return Response.serverError().entity(new ResourceDocument.Builder()
                    .errors(new Errors.Builder()
                            .error(new Error.Builder()
                                    .status(500)
                                    .title(e.getMessage())
                                    .detail(e.toString())
                                    .build())
                            .build()).build()).build();
        }
    }
}
