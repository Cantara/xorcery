package com.exoreaction.xorcery.jsonapi.resources;

import com.exoreaction.xorcery.service.domainevents.api.UUIDs;
import com.exoreaction.xorcery.service.domainevents.api.aggregate.Command;
import com.exoreaction.xorcery.service.domainevents.api.context.DomainContext;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.jsonapi.model.*;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.registry.jsonapi.resources.ResourceContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.exoreaction.xorcery.jsonapi.model.JsonApiRels.self;

public interface CommandsMixin
        extends ResourceContext {

    default Consumer<Links.Builder> schemaLink() {
        return links -> {
        };
    }

    default Consumer<Links.Builder> commandSelfLink()
    {
        return links ->
        {
            links.link(self, getUriInfo().getRequestUri().toASCIIString());
        };
    }

    default Metadata metadata() {
        Metadata.Builder metadata = new Metadata.Builder();

        DomainEventMetadata.Builder request = new DomainEventMetadata.Builder(metadata);

        request.timestamp(System.currentTimeMillis());

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

    // Links
    default Consumer<Links.Builder> commands(UriBuilder baseUriBuilder, DomainContext context) {
        return links ->
        {
            baseUriBuilder.replaceQuery(null);
            for (Command command : context.commands()) {
                if (!Command.isDelete(command.getClass())) {
                    String commandName = Command.getName(command);
                    links.link(commandName, baseUriBuilder.replaceQueryParam("rel", commandName));
                }
            }
        };
    }

    default CompletionStage<ResourceDocument> commandResourceDocument(String rel, String id, DomainContext context) {
        Command command = context.commands().stream().filter(isCommandByName(rel))
                .findFirst().orElseThrow(jakarta.ws.rs.NotFoundException::new);

        ObjectNode result = objectMapper().valueToTree(command);
        result.remove("@class");
        JsonNode jsonId = result.remove("id");
        if (jsonId != null && jsonId.isTextual())
        {
            id = jsonId.textValue();
        }

        return CompletableFuture.completedStage(new ResourceDocument.Builder()
                .links(new Links.Builder().with(commandSelfLink(), schemaLink()))
                .data(new ResourceObject.Builder(Command.getName(command), id)
                        .attributes(new Attributes.Builder().with(a ->
                        {
                            a.attributes(result);
                        })).build())
                .build());
    }

    default CompletionStage<ResourceDocument> commandResourceDocument(String rel, DomainContext context) {
        return commandResourceDocument(rel, null, context);
    }

    default CompletionStage<Response> execute(ResourceObject resourceObject, DomainContext context, Metadata metadata) {

        // Find command based on simple name of class and type in ResourceObject
        String commandName = resourceObject.getType();
        Command command = context.commands().stream().filter(isCommandByName(commandName)).map(c ->
        {
            Class<? extends Command> commandClass = c.getClass();
            try {
                ObjectMapper objectMapper = service(ObjectMapper.class);
                ObjectNode json = resourceObject.getAttributes().json();
                json.set("@class", json.textNode(commandClass.getName()));
                json.set("id", json.textNode(resourceObject.getId()));
                return objectMapper.<Command>treeToValue(json, objectMapper.constructType(commandClass));
            } catch (IOException e) {
                throw new BadRequestException(e);
            }
        }).findFirst().orElseThrow(NotFoundException::new);

        // Transfer over ResourceObject.id as aggregate id if not already set
        if (!metadata.has("aggregateId"))
        {
            String id = resourceObject.getId() != null ? resourceObject.getId() : UUIDs.newId();
            DomainEventMetadata.Builder.aggregateId(id, metadata);
        }

        return context.handle(metadata, command)
                .thenApply(md ->
                {
                    metadata.getString("aggregateId")
                            .ifPresent(id -> md.metadata().set("aggregateId", md.metadata().textNode(id)));
                    return md;
                })
                .thenCompose(md -> ok(md, command))
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

    default Predicate<Command> isCommandByName(String name) {
        return c -> name.equals(Command.getName(c));
    }

    CompletionStage<ResourceDocument> get(String rel);

    default CompletionStage<Response> ok(Metadata metadata, Command command) {
        return get(null).thenApply(rd -> Response.ok(rd).build());
    }

    default CompletionStage<Response> error(ConstraintViolationException e) {
        Errors.Builder errors = new Errors.Builder();
        if (!e.getMessage().equals("")) {
            errors.error(
                    new com.exoreaction.xorcery.jsonapi.model.Error.Builder().title(e.getMessage())
                            .build());
        }
        for (ConstraintViolation<?> entry : e.getConstraintViolations()) {
            errors.error(new com.exoreaction.xorcery.jsonapi.model.Error.Builder()
                    .title(entry.getMessage())
                    .source(new Source.Builder().pointer("/data/attributes/" + entry.getPropertyPath().toString()).build())
                    .build());
        }

        return CompletableFuture.completedStage(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ResourceDocument.Builder().errors(errors.build()).build())
                .build());
    }
}
