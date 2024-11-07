/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.domainevents.jsonapi.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import dev.xorcery.jsonapi.Error;
import dev.xorcery.jsonapi.*;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.util.UUIDs;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static dev.xorcery.jsonapi.JsonApiRels.self;

public interface CommandsResource
        extends CommandsJsonSchemaResource {

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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

        CommandMetadata.Builder request = new CommandMetadata.Builder(metadata);

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
                    links.link(commandName, baseUriBuilder.replaceQueryParam("rel", commandName).toTemplate());
                }
            }
        };
    }

    default CompletionStage<ResourceDocument> commandResourceDocument(String rel, String id, DomainContext context) {
        Command command = context.commands().stream().filter(isCommandByName(rel))
                .findFirst().orElseThrow(jakarta.ws.rs.NotFoundException::new);

        ObjectNode result = objectMapper.valueToTree(command);
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
                ObjectNode json = resourceObject.getAttributes().json();
                json.set("@class", json.textNode(commandClass.getName()));
                if (resourceObject.getId() != null)
                    json.set("id", json.textNode(resourceObject.getId()));
                return objectMapper.<Command>treeToValue(json, objectMapper.constructType(commandClass));
            } catch (IOException e) {
                throw new BadRequestException(e);
            }
        }).findFirst().orElseThrow(NotFoundException::new);

        // Transfer over ResourceObject.id as aggregate id if not already set
        CommandMetadata commandMetadata;
        if (!metadata.has("aggregateId"))
        {
            String id = resourceObject.getId() != null ? resourceObject.getId() : UUIDs.newId();
            commandMetadata = CommandMetadata.Builder.aggregateId(id, metadata);
        } else {
            commandMetadata = new CommandMetadata(metadata);
        }

        return context.handle(commandMetadata, command)
                .thenApply(commandResult ->
                {
                    metadata.getString("aggregateId")
                            .ifPresent(id -> commandResult.metadata().metadata().set("aggregateId", commandResult.metadata().metadata().textNode(id)));
                    return commandResult;
                })
                .thenCompose(this::ok)
                .exceptionallyCompose(this::error);
    }

    default Predicate<Command> isCommandByName(String name) {
        return c -> name.equals(Command.getName(c));
    }

    CompletionStage<ResourceDocument> get(String rel);

    default <T extends Command> CompletionStage<Response> ok(CommandResult<T> commandResult) {
        return get(null).thenApply(rd -> Response.ok(rd).build());
    }

    default CompletionStage<Response> error(Throwable throwable)
    {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof ConstraintViolationException cve) {
            return error(cve);
        } else if (throwable instanceof IllegalArgumentException iae)
        {
            return CompletableFuture.completedStage(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ResourceDocument.Builder().errors(new Errors.Builder()
                            .error(new Error.Builder().title(iae.getMessage()).build())
                            .build()).build())
                    .build());
        }

        return CompletableFuture.failedStage(throwable);
    }

    default CompletionStage<Response> error(ConstraintViolationException e) {
        Errors.Builder errors = new Errors.Builder();
        if (!e.getMessage().equals("")) {
            errors.error(
                    new Error.Builder().title(e.getMessage())
                            .build());
        }
        for (ConstraintViolation<?> entry : e.getConstraintViolations()) {
            errors.error(new Error.Builder()
                    .title(entry.getMessage())
                    .source(new Source.Builder().pointer("/data/attributes/" + entry.getPropertyPath().toString()).build())
                    .build());
        }

        return CompletableFuture.completedStage(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ResourceDocument.Builder().errors(errors.build()).build())
                .build());
    }
}
