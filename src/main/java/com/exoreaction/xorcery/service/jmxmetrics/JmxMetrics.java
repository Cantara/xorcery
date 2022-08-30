package com.exoreaction.xorcery.service.jmxmetrics;

import com.codahale.metrics.jmx.JmxReporter;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.jaxrs.readers.JsonApiMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.rest.RestProcess;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.xorcery.service.registry.api.Registry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
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

    private ServiceResourceObject sro;
    private ReactiveStreams reactiveStreams;
    private Conductor conductor;
    private Registry registry;
    private JsonApiClient client;

    @Inject
    public JmxMetrics(@Named(SERVICE_TYPE) ServiceResourceObject sro,
                      ReactiveStreams reactiveStreams,
                      Conductor conductor,
                      Registry registry,
                      JettyHttpClientContract instance) {
        this.sro = sro;
        this.reactiveStreams = reactiveStreams;
        this.conductor = conductor;
        this.registry = registry;
        Client client = ClientBuilder.newBuilder()
                .withConfig(new ClientConfig()
                        .register(new JsonApiMessageBodyReader(new ObjectMapper()))
                        .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.jmxmetrics")).build())
                        .connectorProvider(new JettyConnectorProvider())
                        .register(instance))
                .build();
        this.client = new JsonApiClient(client);
        this.managementServer = ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public void onStartup(Container container) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        conductor.addConductorListener(new JmxServersConductorListener(sro.serviceIdentifier(), "metrics"));
/*

        reporter = JmxReporter.forRegistry(container.getApplicationHandler().getInjectionManager().getInstance(MetricRegistry.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start();
*/
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        scheduledExecutorService.shutdown();
//        reporter.stop();
    }

    private Map<String, ReactiveEventStreams.Subscription> subscriptions = new ConcurrentHashMap<>();
    private Map<String, Map<String, AtomicReference<JsonNode>>> polledMetrics = new ConcurrentHashMap<>();

    private class MetricEventSubscriber
            implements ReactiveEventStreams.Subscriber<ObjectNode>, EventHandler<Event<ObjectNode>> {
        private final ScheduledExecutorService scheduledExecutorService;
        private String serverId;
        private ReactiveEventStreams.Subscription subscription;
        private Disruptor<Event<ObjectNode>> disruptor;

        public MetricEventSubscriber(ScheduledExecutorService scheduledExecutorService, String serverId) {

            this.scheduledExecutorService = scheduledExecutorService;
            this.serverId = serverId;
        }

        @Override
        public EventSink<Event<ObjectNode>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
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
        public void onEvent(Event<ObjectNode> event, long sequence, boolean endOfBatch) throws Exception {

            Iterator<Map.Entry<String, JsonNode>> fields = event.event.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                AtomicReference<JsonNode> jsonNodeAtomicReference = polledMetrics.get(serverId).get(next.getKey());
                if (jsonNodeAtomicReference != null)
                    jsonNodeAtomicReference.set(next.getValue());
            }

            scheduledExecutorService.schedule(() -> subscription.request(1), 5, TimeUnit.SECONDS);
        }

        @Override
        public void onComplete() {
            disruptor.shutdown();

            try {
                Set<ObjectName> mbeanNames = managementServer.queryNames(ObjectName.getInstance("xorcery:server=" + serverId + ",*"), null);

                for (ObjectName mbeanName : mbeanNames) {
                    managementServer.unregisterMBean(mbeanName);
                }
            } catch (Throwable e) {
                logger.error("Could not unregister MBean", e);
            }
        }
    }

    private class JmxServersConductorListener extends AbstractConductorListener {

        public JmxServersConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
            super(serviceIdentifier, rel);
        }

        public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
            new MetricSynchronization(sro, link).start();
        }
    }

    class MetricSynchronization
            implements RestProcess<Void> {
        private CompletableFuture<Void> result = new CompletableFuture<>();
        private ServiceResourceObject sro;
        private Link link;

        public MetricSynchronization(ServiceResourceObject sro, Link link) {
            this.sro = sro;
            this.link = link;
        }

        @Override
        public void start() {
            client.get(link)
                    .whenComplete((rd, throwable) ->
                    {
                        if (throwable != null)
                        {
                            logger.error("Could not sync server metrics", throwable);
                        } else
                        {
                            // Sync server and metrics beans
                            try {
                                String serverId = sro.getServerId();
                                ObjectName serverName = ObjectName.getInstance("xorcery", "server", serverId);
                                try {
                                    managementServer.getMBeanInfo(serverName);
                                } catch (InstanceNotFoundException e) {
                                    ServerMXBean serverMXBean = new ServerMXBean.Model(serverId);
                                    managementServer.registerMBean(serverMXBean, serverName);
                                }

                                for (ResourceObject resource : rd.getResources().orElseThrow()) {
                                    ObjectName name = ObjectName.getInstance(String.format("xorcery:server=%s,metric=%s", serverId, resource.getId()));
                                    try {
                                        managementServer.getMBeanInfo(name);
                                    } catch (InstanceNotFoundException e) {
                                        String metricType = resource.getAttributes().getString("type").orElseThrow(() ->
                                                new IllegalStateException("Missing type attribute"));
                                        final Supplier<JsonNode> metricSupplier = newMetricSupplier(sro, resource.getId());
                                        Object mBean = switch (metricType) {
                                            case "gauge" -> new GaugeMXBean.Model(metricSupplier);
                                            case "meter" -> new MeterMXBean.Model(metricSupplier);
                                            case "counter" -> new CounterMXBean.Model(metricSupplier);
                                            case "timer" -> new TimerMXBean.Model(metricSupplier);
                                            case "histogram" -> new HistogramMXBean.Model(metricSupplier);
                                            default -> throw new IllegalArgumentException("Unknown type " + metricType);
                                        };
                                        managementServer.registerMBean(mBean, name);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Could not register MXBean", e);
                            }
                        }

                        if (!result.isCancelled()) {
                            scheduledExecutorService.schedule(this::start, 10, TimeUnit.SECONDS);
                        }
                    });
        }

        @Override
        public CompletionStage<Void> result() {
            return result;
        }

        private Supplier<JsonNode> newMetricSupplier(ServiceResourceObject metricsResource, String metricName) {
            return new Supplier<JsonNode>() {
                @Override
                public JsonNode get() {
                    String serverId = metricsResource.getServerId();
                    Map<String, AtomicReference<JsonNode>> currentServerMetrics = polledMetrics.computeIfAbsent(serverId, id -> new ConcurrentHashMap<>());
                    AtomicReference<JsonNode> value = currentServerMetrics.get(metricName);
                    if (value == null) {
                        currentServerMetrics.put(metricName, value = new AtomicReference<>());
                        pollMetrics(metricsResource.getLinkByRel("metricevents").orElseThrow(), serverId, currentServerMetrics.keySet());
                    }
                    return value.get();
                }
            };
        }

        private void pollMetrics(Link metricevents, String serverId, Collection<String> metricNames) {
            ObjectNode parameters = JsonNodeFactory.instance.objectNode();
            parameters.set("metric_names", parameters.textNode(String.join(",", metricNames)));
            reactiveStreams.subscribe(sro.serviceIdentifier(), metricevents, new MetricEventSubscriber(scheduledExecutorService, serverId), new Configuration(parameters), Configuration.empty());
        }


    }

}
