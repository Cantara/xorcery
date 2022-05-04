package com.exoreaction.reactiveservices.server.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//@Provider
public class ServerExceptionMapper
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
            return Response.status(500).build();
        }
    }
}
