package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.jersey.AbstractFeature;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4j.dynamic.DynamicTransactionalContextFactory;
import com.exoreaction.xorcery.service.neo4j.log.Log4jLogProvider;
import com.exoreaction.xorcery.service.neo4j.spi.Neo4jProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.query.TransactionalContextFactory;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
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
            builder.api("neo4j", "api/neo4j");
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
                    .setUserLogProvider(new Log4jLogProvider())
                    .setConfigRaw(settings)
                    .build();

            List<DatabaseConfiguration> databases = configuration().getListAs("neo4jdatabase.databases", json -> new DatabaseConfiguration((ObjectNode) json))
                    .orElseGet(() ->
                            List.of(new DatabaseConfiguration(JsonNodeFactory.instance.objectNode().put("name", "neo4j"))
                            ));
            for (DatabaseConfiguration database : databases) {
                logger.info("Starting Neo4j database:" + database.getName());
                GraphDatabaseService graphDb = null;
                try {
                    graphDb = managementService.database(database.getName());
                } catch (DatabaseNotFoundException e) {
                    managementService.createDatabase(database.getName());
                    graphDb = managementService.database(database.getName());
                }
                GraphDatabase graphDatabase = new GraphDatabase(graphDb, Cypher.defaultFieldMappings());

                // Hack it
/*
                try {
                    Field contextFactoryField = GraphDatabaseFacade.class.getDeclaredField("contextFactory");
                    contextFactoryField.setAccessible(true);
                    contextFactoryField.set(graphDb, new DynamicTransactionalContextFactory((TransactionalContextFactory) contextFactoryField.get(graphDb)));
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
*/

                // Register procedures
                try {
                    GlobalProcedures globalProcedures = ((GraphDatabaseFacade) graphDb).getDependencyResolver().resolveDependency(GlobalProcedures.class);
                    for (Neo4jProvider neo4jProvider : ServiceLoader.load(Neo4jProvider.class)) {
                        globalProcedures.registerProcedure(neo4jProvider.getClass());
                        globalProcedures.registerFunction(neo4jProvider.getClass());
                    }
                } catch (KernelException e) {
                    throw new RuntimeException(e);
                }

                // Run startup Cypher
                for (String cypherResource : database.getStartup()) {
                    try {
                        logger.info("Running Neo4j startup script:" + cypherResource);
                        List<String> statements = Cypher.getCypherStatements(cypherResource);

                        for (String statement : statements) {
                            graphDb.executeTransactionally(statement);
                        }
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                }

                bind(graphDb).named(database.getName()).to(GraphDatabaseService.class);
                bind(graphDatabase).named(database.getName()).to(GraphDatabase.class);
            }

            context.register(new Neo4jService(managementService), GraphDatabases.class, ContainerLifecycleListener.class);

            logger.info("Neo4j initialized");

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

    public record DatabaseConfiguration(ObjectNode json)
            implements JsonElement {
        String getName() {
            return getString("name").orElseThrow();
        }

        List<String> getStartup() {
            return getListAs("startup", JsonNode::textValue).orElse(Collections.emptyList());
        }
    }
}
