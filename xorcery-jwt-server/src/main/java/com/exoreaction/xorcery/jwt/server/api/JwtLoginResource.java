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
package com.exoreaction.xorcery.jwt.server.api;

import com.exoreaction.xorcery.domainevents.jsonapi.resources.CommandsJsonSchemaMixin;
import com.exoreaction.xorcery.jsonapi.*;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.jsonapischema.ResourceDocumentSchema;
import com.exoreaction.xorcery.jsonapischema.ResourceObjectSchema;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.Properties;
import com.exoreaction.xorcery.jsonschema.Types;
import com.exoreaction.xorcery.jsonschema.server.resources.JsonSchemaMixin;
import com.exoreaction.xorcery.jwt.server.JwtConfigurationLoginService;
import com.exoreaction.xorcery.jwt.server.JwtServerConfiguration;
import com.exoreaction.xorcery.jwt.server.JwtService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.self;
import static com.exoreaction.xorcery.jsonapi.MediaTypes.APPLICATION_JSON_API;
import static com.exoreaction.xorcery.jsonapi.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

@Path("api/login")
@Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
public class JwtLoginResource
        extends JsonApiResource
        implements JsonSchemaMixin, CommandsJsonSchemaMixin {

    private final JwtService jwtService;
    private final LoginService loginService;

    @Inject
    public JwtLoginResource(JwtService jwtService, JwtConfigurationLoginService loginService) {
        this.jwtService = jwtService;
        this.loginService = loginService;
    }

    @GET
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(self, getUriInfo().getRequestUri().toASCIIString())
                        .link(describedby, getAbsolutePathBuilder().path(".schema").toTemplate())
                        .build())
                .data(new ResourceObject.Builder("login")
                        .links(new Links.Builder().link("login", getUriInfo().getRequestUri().toASCIIString()))
                        .attributes(new Attributes.Builder().with(a ->
                        {
                            a.attribute("username", "");
                            a.attribute("password", "");
                        })).build())
                .build();
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    public Response post(ResourceObject resourceObject) {

        String username = resourceObject.getAttributes().getString("username").orElse(null);
        String password = resourceObject.getAttributes().getString("password").orElse(null);
        UserIdentity userIdentity = loginService.login(username, password, getHttpServletRequest());
        if (userIdentity == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            String token = jwtService.createJwt(username);
            JwtServerConfiguration jwtServerConfiguration = jwtService.getJwtServerConfiguration();


            Date expiresAt = Date.from(Instant.now().plus(jwtServerConfiguration.getCookieDuration()));

            return Response.ok(new ResourceObject.Builder("jwt").attributes(new Attributes.Builder().attribute("token", token).build()).build())
                    .cookie(new NewCookie.Builder(jwtServerConfiguration.getCookieName())
                            .path(jwtServerConfiguration.getCookiePath())
                            .value(token)
                            .domain(jwtServerConfiguration.getCookieDomain())
                            .expiry(expiresAt)
                            .build())
                    .build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {

        // Login schema
        List<String> required = List.of("username", "password");
        Properties.Builder properties = new Properties.Builder();
        properties.property("username", new JsonSchema.Builder().type(Types.String).title("Username").build());
        properties.property("password", new JsonSchema.Builder().type(Types.String).title("Password").build());

        JsonSchema formSchema = new JsonSchema.Builder()
                .type(Types.Object)
                .properties(new Properties.Builder()
                        .property("data", new JsonSchema.Builder()
                                .type(Types.Object)
                                .additionalProperties(false)
                                .required("type", "data")
                                .properties(new Properties.Builder()
                                        .property("id", new JsonSchema.Builder().type(Types.String).build())
                                        .property("type", new JsonSchema.Builder().constant(JsonNodeFactory.instance.textNode("login")).build())
                                        .property("attributes", new JsonSchema.Builder()
                                                .type(Types.Object)
                                                .required(required.toArray(new String[0]))
                                                .properties(properties.build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        com.exoreaction.xorcery.hyperschema.Link.Builder builder = new com.exoreaction.xorcery.hyperschema.Link.Builder()
                .rel("login")
                .href("{+command_href}")
                .templateRequired("command_href")
                .templatePointer("command_href", "0/data/links/login")
                .submissionMediaType(APPLICATION_JSON_API);

        builder.submissionSchema(formSchema);

        return new ResourceDocumentSchema.Builder()
                .resources(loginSchema())
                .builder()
                .links(new com.exoreaction.xorcery.hyperschema.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .link(builder.build()).build())
                .builder()
                .title("Login")
                .build();
    }

    private ResourceObjectSchema loginSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.login)
                .link("login")
                .attributes(attributes(LoginModel.username, LoginModel.password))
                .with(b -> b.builder().builder().title("Login"))
                .build();
    }
}
