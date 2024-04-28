/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.neo4j;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4j.spi.Neo4jProvider;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.configuration.Config;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilderImplementation;
import org.neo4j.dbms.api.DatabaseNotFoundException;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.transaction.stats.TransactionCounters;
import org.neo4j.logging.log4j.Log4jLogProvider;
import org.neo4j.logging.log4j.Neo4jLoggerContext;
import org.neo4j.server.config.AuthConfigProvider;
import org.neo4j.server.rest.repr.MappingRepresentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

@Service
@Named(Neo4jService.SERVICE_TYPE)
public class Neo4jService
        implements Factory<GraphDatabase>, PreDestroy {

    private static final Logger logger = LogManager.getLogger(Neo4jService.class);

    public static final String SERVICE_TYPE = "neo4jdatabase";

    private final DatabaseManagementService managementService;
    private final GraphDatabase graphDatabase;

    private final LoggerContext loggerContext;

    @Inject
    public Neo4jService(ServiceLocator serviceLocator,
                        Configuration configuration,
                        LoggerContext loggerContext
    ) throws IOException {
        this.loggerContext = loggerContext;
        Neo4jConfiguration neo4jConfiguration = new Neo4jConfiguration(configuration.getConfiguration("neo4jdatabase"));

        Path home = neo4jConfiguration.getDatabasePath();
        logger.info("Neo4j home:" + home);
        File tmpFile = File.createTempFile("neo4jconfig", ".properties");
        tmpFile.deleteOnExit();
        Path tmpConfigFile = tmpFile.toPath();
        try (FileOutputStream out = new FileOutputStream(tmpConfigFile.toFile())) {
            Map<String, String> settings = neo4jConfiguration.settings();
            Properties properties = new Properties();
            properties.putAll(settings);
            properties.store(out, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DatabaseManagementService managementService;

        managementService = createDatabaseManagementService(home, tmpConfigFile);

        logger.info("Starting Neo4j database");
        GraphDatabaseService graphDb;
        graphDb = createDatabase(managementService);

        boolean wipeOnBreakingChanges = neo4jConfiguration.isWipeOnBreakingChanges();
        SemanticVersion targetVersion = neo4jConfiguration.getVersion();
        SemanticVersion currentDatabaseVersion = getExistingDomainVersion(graphDb);
        if (currentDatabaseVersion == null) {
            logger.info("Domain schema version of database does not exist.");
            updateDomainVersionInDatabase(graphDb, targetVersion);
        } else if (targetVersion.isBreakingChange(currentDatabaseVersion)) {
            logger.info("Attempting to update domain schema version of database from {} to {}, this is a breaking change.", currentDatabaseVersion, targetVersion);
            if (wipeOnBreakingChanges) {
                logger.warn("WIPING all data in neo4j projection.");
                managementService.shutdown();
                wipeDatabase(home.resolve("data"));
                managementService = createDatabaseManagementService(home, tmpConfigFile);
                graphDb = createDatabase(managementService);
                updateDomainVersionInDatabase(graphDb, targetVersion);
            } else {
                String msg = String.format("Incompatible database domain schema version, and wipe not allowed. Current version of schema is '%s' and target version is '%s'. Migrate database or set 'neo4jdatabase.domain.wipeOnBreakingChange' to true to allow wipe.", currentDatabaseVersion, targetVersion);
                logger.error(msg);
                throw new IllegalStateException(msg);
            }
        } else if (!currentDatabaseVersion.equals(targetVersion)) {
            logger.info("Automatically updating domain version of database from {} to {}.", currentDatabaseVersion, targetVersion);
            updateDomainVersionInDatabase(graphDb, targetVersion);
        } else {
            logger.info("Target domain schema version matches current database schema version {}", targetVersion);
        }

        // Register procedures
        try {
            GlobalProcedures globalProcedures = ((GraphDatabaseFacade) graphDb).getDependencyResolver().resolveDependency(GlobalProcedures.class);
            globalProcedures.registerComponent(ServiceLocator.class, (ctx) -> serviceLocator, false);

            for (Neo4jProvider neo4jProvider : ServiceLoader.load(Neo4jProvider.class)) {
                globalProcedures.registerProcedure(neo4jProvider.getClass());
                globalProcedures.registerFunction(neo4jProvider.getClass());
                globalProcedures.registerAggregationFunction(neo4jProvider.getClass());
            }
        } catch (KernelException e) {
            throw new RuntimeException(e);
        }

        // Run Cypher schemas
        try {
            Iterable<URL> schemas = Resources.getResources("META-INF/neo4j/schema.cyp");
            for (URL schema : schemas) {
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
        for (String cypherResource : neo4jConfiguration.getStartupCypherStatements()) {
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

        graphDatabase = new GraphDatabase(graphDb, Cypher.defaultFieldMappings());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, graphDb, "neo4j", GraphDatabaseService.class);
        this.managementService = managementService;

        logger.info("Neo4j started");
    }

    private DatabaseManagementService createDatabaseManagementService(Path home, Path tmpConfigFile) {

        return new DatabaseManagementServiceBuilderImplementation(home)
        {
            @Override
            protected Function<GlobalModule, AbstractEditionModule> getEditionFactory(Config config) {
                return globalModule -> new CommunityEditionModule(globalModule)
                {
                    @Override
                    protected AuthConfigProvider createAuthConfigProvider(GlobalModule globalModule) {
                        return new AuthConfigProvider() {
                            @Override
                            public MappingRepresentation getRepresentation() {
                                return null;
                            }
                        };
                    }
                };
            }
        }
        .setUserLogProvider(new Log4jLogProvider(
                new Neo4jLoggerContext(loggerContext, () -> {}, "")))
        .loadPropertiesFromFile(tmpConfigFile)
        .build();
    }

    private static GraphDatabaseService createDatabase(DatabaseManagementService managementService) {
        GraphDatabaseService graphDb;
        try {
            graphDb = managementService.database("neo4j");
        } catch (DatabaseNotFoundException e) {
            managementService.createDatabase("neo4j");
            graphDb = managementService.database("neo4j");
        }
        return graphDb;
    }

    private static SemanticVersion getExistingDomainVersion(GraphDatabaseService graphDb) {
        return graphDb.executeTransactionally("MATCH (n:XorceryProjectionDomainSchema) RETURN n.version AS ver", Map.of(), result -> {
            if (result.hasNext()) {
                Map<String, Object> row = result.next();
                String version = (String) row.get("ver");
                return SemanticVersion.from(version);
            }
            return null;
        });
    }

    /**
     * This method will wipe ALL data in the database, do not use unless you know what you are doing!
     *
     * @param neo4jData the data folder of the neo4j server instance.
     */
    private static void wipeDatabase(Path neo4jData) {
        try {
            Files.walk(neo4jData)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void updateDomainVersionInDatabase(GraphDatabaseService graphDb, SemanticVersion currentVersion) {
        graphDb.executeTransactionally("MERGE (n:XorceryProjectionDomainSchema) SET n.version = $ver RETURN n", Map.of("ver", currentVersion.toString()));
    }

    @Override
    public void preDestroy() {
        managementService.shutdown();
    }

    @Override
    @Singleton
    @Named(Neo4jService.SERVICE_TYPE)
    public GraphDatabase provide() {
        return graphDatabase;
    }

    @Override
    public void dispose(GraphDatabase instance) {
        TransactionCounters txCounters = ((GraphDatabaseFacade) provide().getGraphDatabaseService()).getDependencyResolver().resolveDependency(TransactionCounters.class);
        logger.info("Shutting down Neo4j");
        logger.debug("Currently open transactions:" + txCounters.getNumberOfActiveTransactions());

        // Do nothing
    }
}
