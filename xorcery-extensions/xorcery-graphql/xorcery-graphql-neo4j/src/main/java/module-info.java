module xorcery.graphql.neo4j {
    exports com.exoreaction.xorcery.graphql.jsonapi.api;
    exports com.exoreaction.xorcery.graphql.neo4j.cypher;
    exports com.exoreaction.xorcery.graphql.schema;
    exports com.exoreaction.xorcery.graphql;

    requires xorcery.jsonapi.server;
    requires xorcery.jaxrs.server;
    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires xorcery.configuration.api;
    requires org.glassfish.hk2.api;
    requires com.graphqljava;
}