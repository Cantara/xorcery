package com.exoreaction.xorcery.service.neo4jprojections;

import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.domainevents.DomainEventsConductorListener;
import com.exoreaction.xorcery.service.neo4jprojections.eventstore.EventStoreConductorListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.util.Listeners;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
@Contract
public class Neo4jProjectionsService
        implements ContainerLifecycleListener, Neo4jProjections {

    public static final String SERVICE_TYPE = "neo4jprojections";

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

        @Override
        protected void configure() {
            context.register(Neo4jProjectionsService.class, ContainerLifecycleListener.class, Neo4jProjections.class);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());
    private ReactiveStreams reactiveStreams;
    private Conductor conductor;
    private ServiceResourceObject sro;
    private GraphDatabases graphDatabases;

    private Listeners<ProjectionListener> listeners = new Listeners<>(ProjectionListener.class);

    @Inject
    public Neo4jProjectionsService(Conductor conductor,
                                   ReactiveStreams reactiveStreams,
                                   @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                   GraphDatabases graphDatabases) {
        this.conductor = conductor;
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.graphDatabases = graphDatabases;
    }

    @Override
    public void onStartup(Container container) {
        conductor.addConductorListener(new DomainEventsConductorListener(graphDatabases, reactiveStreams, sro.serviceIdentifier(), "domainevents"));
        conductor.addConductorListener(new EventStoreConductorListener(graphDatabases, reactiveStreams, sro.serviceIdentifier(), "eventstorestreams", listeners));
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void addProjectionListener(ProjectionListener listener)
    {
        listeners.addListener(listener);
    }
}
