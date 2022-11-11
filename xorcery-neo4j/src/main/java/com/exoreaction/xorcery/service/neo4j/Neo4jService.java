package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4j.spi.Neo4jProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.api.DatabaseNotFoundException;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.logging.log4j.Log4jLogProvider;
import org.neo4j.logging.log4j.Neo4jLoggerContext;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Named(Neo4jService.SERVICE_TYPE)
@ContractsProvided({GraphDatabases.class, Factory.class})
public class Neo4jService
        implements Factory<GraphDatabase>, GraphDatabases, PreDestroy {

    private static final Logger logger = LogManager.getLogger(Neo4jService.class);

    public static final String SERVICE_TYPE = "neo4jdatabase";

    private final DatabaseManagementService managementService;

    private final Map<String, GraphDatabase> databases = new ConcurrentHashMap<>();
    private final InstantiationService instantiationService;

    @Inject
    public Neo4jService(ServiceResourceObjects serviceResourceObjects,
                        ServiceLocator serviceLocator,
                        Configuration configuration,
                        InstantiationService instantiationService
    ) {
        this.instantiationService = instantiationService;

        Neo4jConfiguration neo4jConfiguration = new Neo4jConfiguration(configuration.getConfiguration("neo4jdatabase"));

        Path home = neo4jConfiguration.databasePath();
        logger.info("Neo4j home:" + home);
        Map<String, String> settings = neo4jConfiguration.settings();
        managementService = new DatabaseManagementServiceBuilder(home)
                .setUserLogProvider(new Log4jLogProvider(new Neo4jLoggerContext(LogManager.getContext(), () -> {
                })))
//                .loadPropertiesFromFile()setConfigRaw(settings)
                .build();

        List<DatabaseConfiguration> databases = configuration.getListAs("neo4jdatabase.databases", json -> new DatabaseConfiguration((ObjectNode) json))
                .orElseGet(() ->
                        List.of(new DatabaseConfiguration(JsonNodeFactory.instance.objectNode().put("name", "neo4j"))
                        ));
        for (DatabaseConfiguration databaseConfiguration : databases) {
            logger.info("Starting Neo4j database:" + databaseConfiguration.getName());
            GraphDatabaseService graphDb = null;
            try {
                graphDb = managementService.database(databaseConfiguration.getName());
            } catch (DatabaseNotFoundException e) {
                managementService.createDatabase(databaseConfiguration.getName());
                graphDb = managementService.database(databaseConfiguration.getName());
            }

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

            // Run Cypher schemas
            try {
                Enumeration<URL> schemas = ClassLoader.getSystemResources("META-INF/neo4j/schema.cyp");
                while (schemas.hasMoreElements()) {
                    URL schema = schemas.nextElement();
                    try {
                        logger.info("Running Neo4j schema script:" + schema.toExternalForm());
                        List<String> statements = Cypher.getCypherStatements(schema);

                        for (String statement : statements) {
                            graphDb.executeTransactionally(statement);
                        }
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                }
            } catch (Throwable e) {
                logger.error(e);
            }

            // Run startup Cypher
            for (String cypherResource : databaseConfiguration.getStartup()) {
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

            ServiceLocatorUtilities.addOneConstant(serviceLocator, graphDb, databaseConfiguration.getName(), GraphDatabaseService.class);
            ServiceLocatorUtilities.addOneConstant(serviceLocator, new GraphDatabase(graphDb, Cypher.defaultFieldMappings()), databaseConfiguration.getName(), GraphDatabase.class);
        }

        logger.info("Neo4j initialized");

        serviceResourceObjects.publish(new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .api("neo4j", "api/neo4j")
                .build());
    }

    @Override
    public void preDestroy() {
        managementService.shutdown();
    }

    @Override
    public GraphDatabase apply(String name) {
        return databases.computeIfAbsent(name, n -> new GraphDatabase(managementService.database(n), Cypher.defaultFieldMappings()));
    }

    @Override
    @Singleton
    public GraphDatabase provide() {
        return apply("neo4j");
    }

    @Override
    public void dispose(GraphDatabase instance) {
        // Do nothing
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
