package com.exoreaction.xorcery.status.spi;

import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import org.jvnet.hk2.annotations.Contract;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;

@Contract
public interface StatusProvider {
    String getId();

    default ResourceDocument getResourceDocument(String include) {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(describedby, ".schema"))
                .data(new ResourceObject.Builder("status", getId())
                        .links(new Links.Builder()
                                .link("self", "/api/status/"+getId()+"?include={include}")
                                .build())
                        .attributes(new Attributes.Builder()
                                .with(attrs -> addAttributes(attrs, include)))
                        .build()
                )
                .build();
    }

    default void addAttributes(Attributes.Builder attrs, String include)
    {
    }
}
