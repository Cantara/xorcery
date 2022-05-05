package com.exoreaction.reactiveservices.service.neo4j.client;

import java.util.Collections;
import java.util.concurrent.CompletionStage;

public class GraphQuery
{
    private GraphDatabase graphDatabase;


    CompletionStage<GraphResult> execute()
    {
        return graphDatabase.execute(this.toString(), Collections.emptyMap());
    }
}
