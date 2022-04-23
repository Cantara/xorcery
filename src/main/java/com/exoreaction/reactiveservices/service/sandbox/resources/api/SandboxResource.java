package com.exoreaction.reactiveservices.service.sandbox.resources.api;

import com.exoreaction.reactiveservices.jetty.client.ResponseListener;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.rest.RestHelpers;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

//@Path("/")
public class SandboxResource
{
    private HttpClient httpClient;
    private Handlebars handlebars;

    @Inject
    public SandboxResource(HttpClient httpClient, Handlebars handlebars)
    {
        this.httpClient = httpClient;
        this.handlebars = handlebars;
    }

    @GET
    @Produces("text/html")
    public String get(@jakarta.ws.rs.core.Context UriInfo uriInfo) throws ExecutionException, InterruptedException {
        ResponseListener response = new ResponseListener();
        httpClient.newRequest(uriInfo.getRequestUri())
                .method(HttpMethod.GET)
                .accept(ResourceDocument.APPLICATION_JSON_API)
                .send(response);
        return response.getResult()
                        .thenCompose(RestHelpers::toResourceDocument)
                                .thenApply(rd ->
                                {
                                    try {
                                        return handlebars.compile("sandbox/resourcedocument").apply(Context.newContext(rd.object()));
                                    } catch (IOException e) {
                                        throw new CompletionException(e);
                                    }
                                }).toCompletableFuture().get();
    }
}
