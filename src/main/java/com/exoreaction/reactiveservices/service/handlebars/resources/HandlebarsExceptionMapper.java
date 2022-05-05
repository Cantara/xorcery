package com.exoreaction.reactiveservices.service.handlebars.resources;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jetty.client.ResponseListener;
import com.exoreaction.reactiveservices.rest.RestHelpers;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.util.concurrent.CompletionException;

@Provider
public class HandlebarsExceptionMapper
    implements ExceptionMapper<NotAcceptableException>
{
    private HttpClient httpClient;
    private Handlebars handlebars;

    jakarta.inject.Provider<ContainerRequestContext> requestContextProvider;

    @Inject
    public HandlebarsExceptionMapper(HttpClient httpClient, Handlebars handlebars, jakarta.inject.Provider<ContainerRequestContext> requestContextProvider) {
        this.httpClient = httpClient;
        this.handlebars = handlebars;
        this.requestContextProvider = requestContextProvider;
    }

    @Override
    public Response toResponse(NotAcceptableException exception)
    {
        ResponseListener response = new ResponseListener();
        httpClient.newRequest(requestContextProvider.get().getUriInfo().getRequestUri())
                .method(HttpMethod.GET)
                .accept(MediaTypes.APPLICATION_JSON_API)
                .send(response);
        try {
            return response.getResult()
                    .thenCompose(RestHelpers::toResourceDocument)
                    .thenApply(rd ->
                    {
                        try {
                            return Response.
                                    ok().
                                    entity(handlebars.compile("sandbox/resourcedocument").apply(Context.newContext(rd.object()))).
                                    build();
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }).exceptionally(t ->
                    {
                        LogManager.getLogger(getClass()).error(t);
                        throw new CompletionException(t);
                    }).toCompletableFuture().get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
