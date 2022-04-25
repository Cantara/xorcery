package com.exoreaction.reactiveservices.server.resources;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
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

        return Response.status(500).build();
    }
}
