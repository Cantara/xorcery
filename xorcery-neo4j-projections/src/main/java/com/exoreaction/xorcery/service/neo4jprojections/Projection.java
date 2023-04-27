package com.exoreaction.xorcery.service.neo4jprojections;

/**
 * Fields stored in Neo4j for each projection. Updated on each transaction commit when new events have been projected.
 */
public enum Projection {
    id, // Name of projection
    version, // Version of projection
    revision // Revision of incoming event stream
}
