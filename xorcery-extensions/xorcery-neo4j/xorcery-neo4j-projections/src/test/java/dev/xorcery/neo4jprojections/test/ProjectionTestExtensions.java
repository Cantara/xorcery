package dev.xorcery.neo4jprojections.test;

import dev.xorcery.neo4j.spi.Neo4jProvider;
import dev.xorcery.neo4jprojections.Neo4jProjectionHandler;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Procedure;

public class ProjectionTestExtensions
    implements Neo4jProvider
{
    @Context
    public Transaction transaction;

    @Procedure(name = "test.specialSingleEvent")
    public void testSpecialSingleEvent(){
        Neo4jProjectionHandler.ensureIsolatedProjection(transaction);

        System.out.println("This is done in an isolated transaction");
    }
}
