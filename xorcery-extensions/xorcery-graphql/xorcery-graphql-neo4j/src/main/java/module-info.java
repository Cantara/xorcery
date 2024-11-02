module xorcery.graphql.neo4j {
    exports dev.xorcery.graphql.server.jsonapi.api;
    exports dev.xorcery.graphql.server.neo4j.cypher;
    exports dev.xorcery.graphql.server.schema;
    exports dev.xorcery.graphql;

    requires xorcery.jsonapi.server;
    requires xorcery.jaxrs.server;
    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires xorcery.configuration.api;
    requires org.glassfish.hk2.api;
    requires com.graphqljava;
}