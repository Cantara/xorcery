package com.exoreaction.reactiveservices.jaxrs.resources;

import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.Context;
import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.RequestMetadata;
import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.reactiveservices.jsonapi.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

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

    default CompletionStage<JsonElement> commandResourceObject(String rel, Context context) {
        Command command = context.commands().stream().filter(isCommandByName(rel))
                .findFirst().orElseThrow(jakarta.ws.rs.NotFoundException::new);
        return CompletableFuture.completedStage(new ResourceObject.Builder(command.name(), rel)
                .attributes(new Attributes.Builder().with(a ->
                {
                    a.attributes(objectMapper().valueToTree(command));
                })).build());
    }


    default CompletionStage<Response> execute(ResourceObject resourceObject, Context context, Metadata metadata) {

        // Find command based on simple name of class and type in ResourceObject
        String commandName = resourceObject.getType();
        Command command = context.commands().stream().filter(isCommandByName(commandName)).map(c ->
        {
            Class<? extends Command> commandClass = c.getClass();
            try {
                ObjectMapper service = service(ObjectMapper.class);
                ObjectNode json = resourceObject.getAttributes().json();
                json.set("@class", json.textNode(commandClass.getName()));
                return service.<Command>treeToValue(json, service.constructType(commandClass));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).findFirst().orElseThrow(NotFoundException::new);

        // Transfer over ResourceObject.id as aggregate id?
        if (resourceObject.getId() != null)
        {
            metadata = new DomainEventMetadata.Builder(metadata)
                    .aggregateId(resourceObject.getId()).build()
                    .metadata();
        } else
        {
            // Generate new aggregate id
            metadata = new DomainEventMetadata.Builder(metadata)
                    .aggregateId(UUID.randomUUID().toString().replace("-", ""))
                    .build().metadata();
        }

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

    @NotNull
    default Predicate<Command> isCommandByName(String name) {
        return c -> name.equals(c.name());
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
