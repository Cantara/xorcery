package com.exoreaction.reactiveservices.jaxrs.resources;

import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;

public interface JsonApiModelMixin
    extends ResourceContext
{
    default GraphDatabase database()
    {
        return service(GraphDatabase.class);
    }
}
