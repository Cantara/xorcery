package com.exoreaction.reactiveservices.service.neo4j;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.configuration.StandardConfiguration;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabases;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.api.DatabaseNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class Neo4jService
        implements ContainerLifecycleListener, GraphDatabases {
    public static final String SERVICE_TYPE = "neo4jdatabase";

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {

        }

        public Feature() {
        }

        @Override
        protected void configure() {

            StandardConfiguration standardConfiguration = new StandardConfiguration(configuration());

            Map<String, String> neo4jConfig = configuration().getConfiguration("neo4jdatabase.neo4j").asMap()
                    .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                            entry.getValue().textValue()
                    ));

            DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(Path.of(standardConfiguration.home()))
                    .setConfigRaw(neo4jConfig)
                    .build();

            Configuration databases = configuration().getConfiguration("neo4jdatabase.databases");
            for (Map.Entry<String, JsonNode> stringJsonValueEntry : databases.asMap().entrySet()) {
                GraphDatabaseService graphDb = null;
                try {
                    graphDb = managementService.database(stringJsonValueEntry.getKey());
                } catch (DatabaseNotFoundException e) {
                    managementService.createDatabase(stringJsonValueEntry.getKey());
                    graphDb = managementService.database(stringJsonValueEntry.getKey());
                }
                GraphDatabase graphDatabase = new GraphDatabase(graphDb, Cypher.defaultMappingReturn());

                bind(graphDb).named(stringJsonValueEntry.getKey()).to(GraphDatabaseService.class);
                bind(graphDatabase).named(stringJsonValueEntry.getKey()).to(GraphDatabase.class);
            }

            context.register(new Neo4jService(managementService), GraphDatabases.class, ContainerLifecycleListener.class);
        }

    }

    private final DatabaseManagementService managementService;

    private final Map<String, GraphDatabase> databases = new ConcurrentHashMap<>();

    public Neo4jService(DatabaseManagementService managementService) {
        this.managementService = managementService;
    }

    @Override
    public void onStartup(Container container) {
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        managementService.shutdown();
    }

    @Override
    public GraphDatabase apply(String name) {
        return databases.computeIfAbsent(name, n -> new GraphDatabase(managementService.database(n), Cypher.defaultMappingReturn()));
    }
}
