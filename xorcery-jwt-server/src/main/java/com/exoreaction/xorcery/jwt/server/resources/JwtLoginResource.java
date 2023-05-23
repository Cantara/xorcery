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
package com.exoreaction.xorcery.jwt.server.resources;

import com.exoreaction.xorcery.domainevents.jsonapi.resources.CommandsJsonSchemaMixin;
import com.exoreaction.xorcery.domainevents.jsonapi.resources.CommandsMixin;
import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.jsonapischema.ResourceDocumentSchema;
import com.exoreaction.xorcery.jsonapischema.ResourceObjectSchema;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.server.resources.JsonSchemaMixin;
import com.exoreaction.xorcery.jwt.server.JwtJsonApiService;
import com.exoreaction.xorcery.jetty.server.security.jwt.JwtUserPrincipal;
import io.jsonwebtoken.Jwts;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.UserIdentity;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.xorcery.jsonapi.MediaTypes.APPLICATION_JSON_API;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.self;

@Path("api/login")
public class JwtLoginResource
        extends JsonApiResource
        implements JsonSchemaMixin, CommandsJsonSchemaMixin, CommandsMixin {

    private final JwtJsonApiService jsonApiService;

    @Inject
    public JwtLoginResource(JwtJsonApiService jsonApiService) {
        this.jsonApiService = jsonApiService;
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return new ResourceDocumentSchema.Builder()
                .resources(loginSchema())
                .builder()
                .links(new com.exoreaction.xorcery.hyperschema.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .build())
                .builder()
                .title("Login")
                .build();
    }

    @GET
    public CompletionStage<ResourceDocument> get(@QueryParam("rel") String rel) {
        return CompletableFuture.completedStage(new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(self, getUriInfo().getRequestUri().toASCIIString())
                        .link(describedby, getAbsolutePathBuilder().path(".schema").toTemplate())
                        .build())
                .data(new ResourceObject.Builder("login")
                        .attributes(new Attributes.Builder().with(a ->
                        {
                            a.attribute("username", null);
                            a.attribute("password", null);
                        })).build())
                .build());
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    public Response post(ResourceObject resourceObject) {

        UserIdentity userIdentity = jsonApiService.getLoginService().login(resourceObject.getAttributes().getString("username").orElse(null), resourceObject.getAttributes().getString("password").orElse(null), getHttpServletRequest());
        if (userIdentity == null)
        {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        JwtUserPrincipal principal = (JwtUserPrincipal) userIdentity.getUserPrincipal();
        io.jsonwebtoken.Claims claims = principal.getClaims();

        String token = Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setIssuer(getHttpServletRequest().getHeader(HttpHeader.HOST.lowerCaseName()))
                .setIssuedAt(new Date())
                .setSubject(principal.getName())
                .signWith(jsonApiService.getSigningKey())
                .compact();
        return Response.ok(new ResourceObject.Builder("jwt").attributes(new Attributes.Builder().attribute("token", token).build()).build())
                .cookie(new NewCookie.Builder("token").value(token).build())
                .build();
    }

    private ResourceObjectSchema loginSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.login)
                .attributes(attributes(LoginModel.username, LoginModel.password))
                .with(b -> b.builder().builder().title("Login"))
                .build();
    }
}
