package com.exoreaction.xorcery.service.domainevents.neo4jprojections;


import com.exoreaction.xorcery.service.neo4j.spi.Neo4jProvider;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.Map;

public class ApplyJsonDomainEvent
    implements Neo4jProvider
{

    @Context
    public Transaction transaction;

    @Description("Apply JsonDomainEvent")
    @Procedure(
            name = "domainevent.jsondomainevent",
            mode = Mode.WRITE
    )
    public void applyJsonDomainEvent(@Name(value = "metadata") Map<String, Object> metadata,
                                     @Name(value = "created",defaultValue = "{}") Map<String, Object> created,
                                     @Name(value = "updated",defaultValue = "{}") Map<String, Object> updated,
                                     @Name(value = "deleted",defaultValue = "{}") Map<String, Object> deleted,
                                     @Name(value = "attributes",defaultValue = "{}") Map<String, Object> attributes,
                                     @Name(value = "addedrelationships",defaultValue = "[]") List<Map<String, Object>> addedrelationships,
                                     @Name(value = "removedrelationships",defaultValue = "[]") List<Map<String, Object>> removedrelationships
                                     ) {

        System.out.println(metadata);
    }

}
