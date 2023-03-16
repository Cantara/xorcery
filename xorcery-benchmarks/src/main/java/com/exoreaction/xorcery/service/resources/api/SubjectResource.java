package com.exoreaction.xorcery.service.resources.api;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import javax.security.auth.Subject;
import java.security.Principal;

@Path("api/subject")
public class SubjectResource
        extends JsonApiResource {

    @GET
    public ResourceDocument get() {

        Subject subject = getSubject();

        return new ResourceDocument.Builder().data(new ResourceObject.Builder("subject").attributes(new Attributes.Builder().with(b ->
        {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            for (Principal principal : subject.getPrincipals()) {
                arrayNode.add(principal.getName());
            }
            b.attribute("principals", arrayNode);
            arrayNode = JsonNodeFactory.instance.arrayNode();
            for (Object credential : subject.getPrivateCredentials()) {
                arrayNode.add(credential.toString());
            }
            b.attribute("principals", arrayNode);

        }).build()).build()).build();
    }
}
