package com.exoreaction.reactiveservices.service.jmxmetrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jaxrs.readers.JsonApiMessageBodyReader;
import com.exoreaction.reactiveservices.jsonapi.client.JsonApiClient;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class JmxMetrics
        implements ContainerLifecycleListener {
    public static final String SERVICE_TYPE = "jmxmetrics";
    public static final Marker MARKER = MarkerManager.getMarker("service:" + SERVICE_TYPE);

    private final Logger logger = LogManager.getLogger(getClass());
    private final MBeanServer managementServer;
    private ScheduledExecutorService scheduledExecutorService;
    private JmxReporter reporter;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void configure() {
            context.register(JmxMetrics.class, ContainerLifecycleListener.class);
        }
    }

    private ReactiveStreams reactiveStreams;
    private Registry registry;
    private JsonApiClient client;

    @Inject
    public JmxMetrics(ReactiveStreams reactiveStreams, Registry registry) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
        ClientConfig config = new ClientConfig();
        config.register(new JsonApiMessageBodyReader());
        Client client = ClientBuilder.newBuilder().withConfig(config).build();
        this.client = new JsonApiClient(client);
        this.managementServer = ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public void onStartup(Container container) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        registry.addRegistryListener(new JmxServersRegistryListener());

        reporter = JmxReporter.forRegistry(container.getApplicationHandler().getInjectionManager().getInstance(MetricRegistry.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start();

    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        scheduledExecutorService.shutdown();
        reporter.stop();
    }

    private void fetchMetrics(Link serverMetrics, ServiceResourceObject resourceObject) {
        client.get(serverMetrics)
                .whenComplete((rd, throwable) ->
                {
                    // Create server and metrics beans
                    try {
                        String serverId = resourceObject.serverId();
                        ServerMXBean serverMXBean = new ServerMXBean.Model(serverId);
                        managementServer.registerMBean(serverMXBean, ObjectName.getInstance("reactive", "server", serverId));

                        for (ResourceObject resource : rd.getResources().orElseThrow().getResources()) {
                            ObjectName name = ObjectName.getInstance(String.format("reactive:server=%s,metric=%s", serverId, resource.getId()));
                            String metricType = resource.getAttributes().getString("type");
                            final Supplier<JsonValue> metricSupplier = newMetricSupplier(resourceObject, resource.getId());
                            Object mBean = switch (metricType) {
                                case "gauge" -> new GaugeMXBean.Model(metricSupplier);
                                case "meter" -> new MeterMXBean.Model(metricSupplier);
                                case "counter" -> new CounterMXBean.Model(metricSupplier);
                                case "timer" -> new TimerMXBean.Model(metricSupplier);
                                default -> throw new IllegalArgumentException("Unknown type "+metricType);
                            };
                            managementServer.registerMBean(mBean, name);

                        }
                    } catch (Exception e) {
                        logger.error("Could not register MXBean", e);
                    }
                });
    }

    private Map<String, ReactiveEventStreams.Subscription> subscriptions = new ConcurrentHashMap<>();
    private Map<String, Map<String, AtomicReference<JsonValue>>> polledMetrics = new ConcurrentHashMap<>();

    private Supplier<JsonValue> newMetricSupplier(ServiceResourceObject metricsResource, String metricName) {
        return new Supplier<JsonValue>() {
            @Override
            public JsonValue get() {
                String serverId = metricsResource.serverId();
                Map<String, AtomicReference<JsonValue>> currentServerMetrics = polledMetrics.computeIfAbsent(serverId, id -> new ConcurrentHashMap<>());
                AtomicReference<JsonValue> value = currentServerMetrics.get(metricName);
                if (value == null) {
                    currentServerMetrics.put(metricName, value = new AtomicReference<>());
                    pollMetrics(metricsResource.linkByRel("metricevents").orElseThrow(), serverId, currentServerMetrics.keySet());
                }
                return value.get();
            }
        };
    }

    private void pollMetrics(Link metricevents, String serverId, Collection<String> metricNames) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("metric_names", String.join(",", metricNames));
        reactiveStreams.subscribe(metricevents, new MetricEventSubscriber(scheduledExecutorService, serverId), parameters, MARKER);
    }

    private class MetricEventSubscriber
            implements ReactiveEventStreams.Subscriber<JsonObject>, EventHandler<Event<JsonObject>> {
        private final ScheduledExecutorService scheduledExecutorService;
        private String serverId;
        private ReactiveEventStreams.Subscription subscription;
        private Disruptor<Event<JsonObject>> disruptor;

        public MetricEventSubscriber(ScheduledExecutorService scheduledExecutorService, String serverId) {

            this.scheduledExecutorService = scheduledExecutorService;
            this.serverId = serverId;
        }

        @Override
        public EventSink<Event<JsonObject>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;
            Optional.ofNullable(subscriptions.put(serverId, subscription))
                    .ifPresent(ReactiveEventStreams.Subscription::cancel);

            disruptor = new Disruptor<>(Event::new, 8, new NamedThreadFactory("JmxMetrics-"));
            disruptor.handleEventsWith(this);
            disruptor.start();

            subscription.request(1);

            return disruptor.getRingBuffer();
        }

        @Override
        public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch) throws Exception {

            event.event.forEach((name, value) ->
            {
                polledMetrics.get(serverId).get(name).set(value);
            });

//            System.out.println("Metric:"+event.event.toString()+":"+event.metadata);
            scheduledExecutorService.schedule(() -> subscription.request(1), 5, TimeUnit.SECONDS);
        }

        @Override
        public void onComplete() {
            disruptor.shutdown();
        }
    }

    private class JmxServersRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("metrics").ifPresent(link -> fetchMetrics(link, new ServiceResourceObject(service)));
        }
    }

}
