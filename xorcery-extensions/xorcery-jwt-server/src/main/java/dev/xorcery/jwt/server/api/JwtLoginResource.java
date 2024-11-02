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
package dev.xorcery.jwt.server.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.domainevents.jsonapi.resources.CommandsJsonSchemaResource;
import dev.xorcery.jaxrs.server.resources.BaseResource;
import dev.xorcery.jsonapi.*;
import dev.xorcery.jsonapischema.ResourceDocumentSchema;
import dev.xorcery.jsonapischema.ResourceObjectSchema;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.Properties;
import dev.xorcery.jsonschema.Types;
import dev.xorcery.jsonschema.server.resources.JsonSchemaResource;
import dev.xorcery.jwt.server.JwtConfigurationLoginService;
import dev.xorcery.jwt.server.JwtServerConfiguration;
import dev.xorcery.jwt.server.JwtService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.security.LoginService;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static dev.xorcery.jsonapi.JsonApiRels.describedby;
import static dev.xorcery.jsonapi.JsonApiRels.self;
import static dev.xorcery.jsonapi.MediaTypes.APPLICATION_JSON_API;
import static dev.xorcery.jsonapi.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

@Path("api/login")
@Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
public class JwtLoginResource
        extends BaseResource
        implements JsonSchemaResource, CommandsJsonSchemaResource {

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
        try {
            getHttpServletRequest().login(username, password);
        } catch (ServletException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            String token = jwtService.createJwt(username);
            JwtServerConfiguration jwtServerConfiguration = jwtService.getJwtServerConfiguration();


            Date expiresAt = Date.from(Instant.now().plus(jwtServerConfiguration.getCookieDuration()));
            NewCookie tokenCookie = new NewCookie.Builder(jwtServerConfiguration.getCookieName())
                    .path(jwtServerConfiguration.getCookiePath())
                    .value(token)
                    .domain(jwtServerConfiguration.getCookieDomain())
                    .expiry(expiresAt)
                    .build();

            return resourceObject.getAttributes().getString("uri").map(uri ->
                    Response.temporaryRedirect(URI.create(uri)).cookie(tokenCookie).build()).orElseGet(() ->
                    Response.ok(new ResourceObject.Builder("jwt")
                                    .attributes(new Attributes.Builder().attribute("token", token).build()).build())
                            .cookie(tokenCookie)
                            .build());
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
        properties.property("uri", new JsonSchema.Builder().type(Types.String).title("Redirect URI on success").build());

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

        dev.xorcery.hyperschema.Link.Builder builder = new dev.xorcery.hyperschema.Link.Builder()
                .rel("login")
                .href("{+command_href}")
                .templateRequired("command_href")
                .templatePointer("command_href", "0/data/links/login")
                .submissionMediaType(APPLICATION_JSON_API);

        builder.submissionSchema(formSchema);

        return new ResourceDocumentSchema.Builder()
                .resources(loginSchema())
                .builder()
                .links(new dev.xorcery.hyperschema.Links.Builder()
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
