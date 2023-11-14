package com.exoreaction.xorcery.neo4jprojections.streams;

/**
 * A preprocessor can throw this to indicate that the current batch should be released, and the event reprocessed.
 *
 * One cornercase is to allow an event processor to be ensured that any database transaction is done against a projection
 * which has all previous events written to it already.
 */
public class EarlyReleaseException
    extends RuntimeException
{
    public static final EarlyReleaseException INSTANCE = new EarlyReleaseException();
}
