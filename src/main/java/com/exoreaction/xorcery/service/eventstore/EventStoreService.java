package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.eventstore.model.StreamModel;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.*;

@Contract
@Singleton
public class EventStoreService
        implements
        ContainerLifecycleListener {

    private static final Logger logger = LogManager.getLogger(EventStoreService.class);

    public static final String SERVICE_TYPE = "eventstore";
    private final EventStoreDBClient client;
    private final ObjectMapper objectMapper;
    private ServiceResourceObject sro;
    private ReactiveStreams2 reactiveStreams;
    private Conductor conductor;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api(EventStoreRels.eventstore.name(), "api/eventstore");
            builder.websocket(EventStoreRels.writeevents.name(), "ws/writeevents");
            builder.websocket(EventStoreRels.readevents.name(), "ws/readevents");
        }

        @Override
        protected void configure() {
            context.register(EventStoreService.class, EventStoreService.class, ContainerLifecycleListener.class);
        }
    }

    @Inject
    public EventStoreService(@Named(SERVICE_TYPE) ServiceResourceObject sro,
                             Configuration configuration, ReactiveStreams2 reactiveStreams,
                             Conductor conductor) throws ParseError {
        this.sro = sro;
        this.reactiveStreams = reactiveStreams;
        this.conductor = conductor;
        this.objectMapper = new ObjectMapper();
        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        client = EventStoreDBClient.create(settings);

        // Test connection
        StreamMetadata metadata = client.getStreamMetadata("$all").join();
        logger.info("$all stream metadata:" + metadata.toString());
    }

    @Override
    public void onStartup(Container container) {
        // Read
        sro.getLinkByRel(EventStoreRels.readevents.name()).ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new EventStorePublisher(client, objectMapper, cfg));
        });

        // Write
        sro.getLinkByRel(EventStoreRels.writeevents.name()).ifPresent(link ->
        {
            reactiveStreams.subscriber(link.getHrefAsUri().getPath(), cfg -> new EventStoreSubscriber(client, cfg));
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public CompletionStage<StreamModel> getStream(String id) {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, 1, ReadStreamOptions.get().backwards().fromEnd()),
                (streamMetaData, readResult) ->
                {

                    return new StreamModel(id, readResult.getEvents().stream().findFirst().map(event -> event.getEvent().getStreamRevision().getValueUnsigned()).orElse(-1L));
                });
    }
}
