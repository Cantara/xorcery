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
package com.exoreaction.xorcery.test.resources;

import com.exoreaction.xorcery.jaxrs.server.resources.BaseResource;
import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import javax.security.auth.Subject;
import java.security.Principal;

@Path("api/subject")
public class SubjectResource
        extends BaseResource
        implements JsonApiResource {

    @GET
    public ResourceDocument get() {

        Subject subject = getSubject().orElseThrow(ForbiddenException::new);

        return new ResourceDocument.Builder().data(new ResourceObject.Builder("subject").attributes(new Attributes.Builder().with(b ->
        {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            for (Principal principal : subject.getPrincipals()) {
                arrayNode.add(principal.getName());
            }
            b.attribute("names", arrayNode);
            arrayNode = JsonNodeFactory.instance.arrayNode();
            for (Object credential : subject.getPrivateCredentials()) {
                arrayNode.add(credential.toString());
            }
            b.attribute("credentials", arrayNode);

        }).build()).build()).build();
    }
}
