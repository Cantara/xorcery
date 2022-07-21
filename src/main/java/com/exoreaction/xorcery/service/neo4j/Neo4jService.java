package com.exoreaction.xorcery.service.neo4j;

import apoc.merge.Merge;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.api.DatabaseNotFoundException;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.kernel.api.Procedures;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class Neo4jService
        implements ContainerLifecycleListener, GraphDatabases {

    private static final Logger logger = LogManager.getLogger(Neo4jService.class);

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
            builder.api("neo4j","api/neo4j");
        }

        public Feature() {
        }

        @Override
        protected void configure() {

            Neo4jConfiguration neo4jConfiguration = new Neo4jConfiguration(configuration().getConfiguration("neo4jdatabase"));

            Path home = neo4jConfiguration.databasePath();
            logger.info("Neo4j home:" + home);
            Map<String, String> settings = neo4jConfiguration.settings();
            DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(home)
                    .setConfigRaw(settings)
                    .build();

            List<String> databases = configuration().getListAs("neo4jdatabase.databases", JsonNode::textValue).orElse(List.of("neo4j"));
            for (String database : databases) {
                GraphDatabaseService graphDb = null;
                try {
                    graphDb = managementService.database(database);
                } catch (DatabaseNotFoundException e) {
                    managementService.createDatabase(database);
                    graphDb = managementService.database(database);
                }
                GraphDatabase graphDatabase = new GraphDatabase(graphDb, Cypher.defaultFieldMappings());

                bind(graphDb).named(database).to(GraphDatabaseService.class);
                bind(graphDatabase).named(database).to(GraphDatabase.class);
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
        return databases.computeIfAbsent(name, n -> new GraphDatabase(managementService.database(n), Cypher.defaultFieldMappings()));
    }
}
