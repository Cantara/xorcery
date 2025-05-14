package dev.xorcery.neo4jprojections.spi;

/**
 * Neo4jEventProjection implementations can throw this to indicate that the current event should be the only one in the batch,
 * so all previous ones needs to be committed first, and no other event can be executed after in the same transaction.
 */
public class SignalProjectionIsolationException
    extends RuntimeException
{
}
