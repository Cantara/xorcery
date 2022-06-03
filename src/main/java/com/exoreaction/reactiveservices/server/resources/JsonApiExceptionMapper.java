package com.exoreaction.reactiveservices.server.resources;

import com.exoreaction.reactiveservices.jsonapi.model.Error;
import com.exoreaction.reactiveservices.jsonapi.model.Errors;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Provider
@Produces(APPLICATION_JSON_API)
public class JsonApiExceptionMapper
    implements ExceptionMapper<Throwable>
{
    Logger logger = LogManager.getLogger(getClass());
    @Override
    public Response toResponse(Throwable exception)
    {
        logger.error("Response exception", exception);

        try {
            throw exception;
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (Throwable e) {
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
