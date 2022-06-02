package com.exoreaction.reactiveservices.jaxrs.resources;

import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.Context;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.RequestMetadata;
import com.exoreaction.reactiveservices.jsonapi.model.Errors;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.Source;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface JsonApiCommandMixin
        extends ResourceContext {
    default Metadata metadata() {
        Metadata.Builder metadata = new Metadata.Builder();

        RequestMetadata.Builder request = new RequestMetadata.Builder(metadata);

/* TODO Do this later but set JWT Claims as metadata
        if ( getSecurityContext().getUserPrincipal() != null )
        {
            metadata.set( StandardMetadata.USER, getSecurityContext().getUserPrincipal().getName() );

            if ( getSecurityContext().getUserPrincipal() instanceof RealVisionPrincipal )
            {
                RealVisionPrincipal userPrincipal = (RealVisionPrincipal) getSecurityContext().getUserPrincipal();

                userPrincipal.populateMetadata( metadata );
            }
        }
*/

        String forwardedFor = getContainerRequestContext().getHeaderString("X-Forwarded-For");

        if (forwardedFor != null) {
            String remoteClient = forwardedFor.split(",")[0].trim();

            request.remoteIp(remoteClient);
        }

        String agent = getContainerRequestContext().getHeaderString(HttpHeaders.USER_AGENT);

        if (agent != null) {
            request.agent(agent);
        }

        return metadata.build();
    }

    default CompletionStage<Response> execute(Metadata metadata, ResourceDocument resourceDocument, Context context) {
        String profile = getFirstQueryParameter("profile");

        Command command = context.commands().stream().filter(c ->
        {
            Class<?> commandClass = c.getClass();
            return profile.equals(commandClass.getSimpleName());
        }).map(c ->
        {
            Class<? extends Command> commandClass = c.getClass();
            ResourceObject ro = resourceDocument.getResource().orElseThrow(BadRequestException::new);
            try {
                ObjectMapper service = service(ObjectMapper.class);
                ObjectNode json = ro.getAttributes().json();
                json.set("@class", json.textNode(commandClass.getName()));
                return service.<Command>treeToValue(json, service.constructType(commandClass));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).findFirst().orElseThrow(NotFoundException::new);

        return context.handle(metadata, command)
                .thenCompose(this::ok)
                .exceptionallyCompose(throwable ->
                {
                    while (throwable.getCause() != null) {
                        throwable = throwable.getCause();
                    }

                    // Todo integrate with Bean Validation 3.0 here
                    if (throwable instanceof ConstraintViolationException cve) {
                        return error(cve);
                    }

                    return CompletableFuture.failedStage(throwable);
                });
    }

    CompletionStage<Response> ok(Metadata metadata);

    default CompletionStage<Response> error(ConstraintViolationException e) {
        Errors.Builder errors = new Errors.Builder();
        if (!e.getMessage().equals("")) {
            errors.error(
                    new com.exoreaction.reactiveservices.jsonapi.model.Error.Builder().title(e.getMessage())
                            .build());
        }
        for (ConstraintViolation<?> entry : e.getConstraintViolations()) {
            errors.error(new com.exoreaction.reactiveservices.jsonapi.model.Error.Builder()
                    .title(entry.getMessage())
                    .source(new Source.Builder().pointer("/data/attributes/" + entry.getPropertyPath().toString()).build())
                    .build());
        }

        return CompletableFuture.completedStage(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ResourceDocument.Builder().errors(errors.build()).build())
                .build());
    }

}
