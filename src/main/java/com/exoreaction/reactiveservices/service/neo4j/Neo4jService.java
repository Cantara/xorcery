package com.exoreaction.reactiveservices.service.neo4j;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.configuration.StandardConfiguration;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.service.mapdbdomainevents.MapDbDomainEventsService;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonString;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

@Singleton
public class Neo4jService
    implements ContainerLifecycleListener
{
    public static final String SERVICE_TYPE = "neo4jdatabase";

    @Provider
    public static class Feature
        extends AbstractFeature
    {
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

            Map<String,String> neo4jConfig = configuration().getConfiguration("neo4jdatabase").getConfiguration("neo4j").asMap()
                    .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                                    switch (entry.getValue().getValueType()) {
                                        case STRING -> ((JsonString) entry.getValue()).getString();
                                        default -> entry.getValue().toString();
                                    }
                            ));

            DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(Path.of(standardConfiguration.home()) )
                    .setConfigRaw(neo4jConfig)
                    .build();

            GraphDatabaseService graphDb = managementService.database( DEFAULT_DATABASE_NAME );
            GraphDatabase graphDatabase = new GraphDatabase(graphDb);
            context.register(new Neo4jService(managementService, graphDb, graphDatabase), ContainerLifecycleListener.class);
            bind(graphDb).to(GraphDatabaseService.class);
            bind(graphDatabase).to(GraphDatabase.class);
        }

    }

    private final DatabaseManagementService managementService;
    private final GraphDatabaseService graphDb;
    private final GraphDatabase graphDatabase;

    public Neo4jService(DatabaseManagementService managementService, GraphDatabaseService graphDb, GraphDatabase graphDatabase) {
        this.managementService = managementService;
        this.graphDb = graphDb;
        this.graphDatabase = graphDatabase;
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
}
