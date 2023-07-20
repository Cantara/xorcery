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
package com.exoreaction.xorcery.jmxmetrics;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.management.*;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Register subscriber which publishers of metrics can connect to, and exposes published metrics in local JMX MBeanServer. This allows showing metrics
 * of other services in VisualVM connected to local JVM.
 *
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service
@Named(JmxMetricsService.SERVICE_TYPE)
@RunLevel(6)
public class JmxMetricsService
        implements PreDestroy {

    public static final String SERVICE_TYPE = "jmxmetrics";

    private final MBeanServer managementServer;
    private final CompletionStage<Void> result;
    private final JMXConnectorServer jmxConnectorServer;
    private final Logger logger;
    private final Registry registry;
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    public JmxMetricsService(ReactiveStreamsServer reactiveStreamsServer,
                             Configuration configuration, Logger logger) throws IOException {
        this.logger = logger;

        JmxMetricsConfiguration jmxMetricsConfiguration = new JmxMetricsConfiguration(configuration.getConfiguration("jmxmetrics"));
        this.managementServer = ManagementFactory.getPlatformMBeanServer();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        registry = LocateRegistry.createRegistry(jmxMetricsConfiguration.getPort());
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL(jmxMetricsConfiguration.getURI());
        jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        jmxConnectorServer.start();
        logger.info("JMX metrics available at: {}", jmxMetricsConfiguration.getExternalURI());

        result = reactiveStreamsServer.subscriber("metrics",
                cfg -> new MetricsSubscriber(scheduledExecutorService, cfg.getString("instance.id").orElse("unknown")),
                MetricsSubscriber.class);
    }

    @Override
    public void preDestroy() {
        try {
            jmxConnectorServer.stop();
        } catch (IOException e) {
            logger.warn("Could not stop JMX connector", e);
        }
        scheduledExecutorService.shutdown();
        result.toCompletableFuture().complete(null);
    }

    private Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private Map<String, Map<String, AtomicReference<ObjectNode>>> polledMetrics = new ConcurrentHashMap<>();

    private class MetricsSubscriber
            implements Subscriber<WithMetadata<ObjectNode>>, EventHandler<WithMetadata<ObjectNode>> {
        private final ScheduledExecutorService scheduledExecutorService;
        private String serverId;
        private Subscription subscription;
        private Disruptor<WithMetadata<ObjectNode>> disruptor;
        private ObjectName serverName;

        public MetricsSubscriber(ScheduledExecutorService scheduledExecutorService, String serverId) {

            this.scheduledExecutorService = scheduledExecutorService;
            this.serverId = serverId;
            polledMetrics.put(serverId, new ConcurrentHashMap<>());
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            Optional.ofNullable(subscriptions.put(serverId, subscription))
                    .ifPresent(Subscription::cancel);

            disruptor = new Disruptor<>(WithMetadata::new, 8, new NamedThreadFactory("JmxMetrics-"));
            disruptor.handleEventsWith(this);
            disruptor.start();

            subscription.request(1);
        }

        private void cleanup() {
            try {
                Set<ObjectName> mbeanNames = managementServer.queryNames(ObjectName.getInstance("remote:server=" + serverId + ",*"), null);

                for (ObjectName mbeanName : mbeanNames) {
                    managementServer.unregisterMBean(mbeanName);
                }
            } catch (Throwable e) {
                logger.error("Could not unregister MBean", e);
            }
        }

        @Override
        public void onNext(WithMetadata<ObjectNode> item) {
            disruptor.publishEvent((ref, seq, e) -> ref.set(e), item);
        }

        @Override
        public void onEvent(WithMetadata<ObjectNode> event, long sequence, boolean endOfBatch) throws Exception {

            try {
                Iterator<Map.Entry<String, JsonNode>> fields = event.event().fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> next = fields.next();
                    ObjectNode value = (ObjectNode) next.getValue();
//                    logger.info(next.getKey() + ":" + next.getValue());
                    AtomicReference<ObjectNode> jsonNodeAtomicReference = getMBeanMetrics(serverId, next.getKey(), value);
                    jsonNodeAtomicReference.set(value);
                }

                scheduledExecutorService.schedule(() -> subscription.request(1), 5, TimeUnit.SECONDS);
            } catch (Throwable e) {
                cleanup();
                subscription.cancel();
            }
        }

        private AtomicReference<ObjectNode> getMBeanMetrics(String serverId, String key, ObjectNode values) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {

            AtomicReference<ObjectNode> metrics = polledMetrics.get(serverId).get(key);

            if (metrics == null) {
                metrics = new AtomicReference<>();
                String[] names = key.split(":", 2);
                String domainName = names[0];
                String name = "remote:" + names[1] + ",domain=" + domainName + ",server=" + serverId;

                List<MBeanAttributeInfo> attributeInfos = new ArrayList<>();
                List<Function<JsonNode, Object>> converters = new ArrayList<>();
                Iterator<String> fieldNames = values.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode value = values.get(fieldName);
                    if (value instanceof TextNode) {
                        attributeInfos.add(new MBeanAttributeInfo(fieldName, String.class.getName(), fieldName, true, false, false));
                        converters.add(JsonNode::asText);
                    } else if (value instanceof NumericNode) {
                        attributeInfos.add(new MBeanAttributeInfo(fieldName, Number.class.getName(), fieldName, true, false, false));
                        converters.add(JsonNode::numberValue);
                    } else if (value instanceof BooleanNode) {
                        attributeInfos.add(new MBeanAttributeInfo(fieldName, Boolean.class.getName(), fieldName, true, false, false));
                        converters.add(JsonNode::booleanValue);
                    } else if (value instanceof ObjectNode) {
                        attributeInfos.add(new MBeanAttributeInfo(fieldName, Map.class.getName(), fieldName, true, false, false));
                        converters.add(v -> JsonElement.toMap((ObjectNode) v, JsonNode::textValue));
                    } else if (value instanceof ArrayNode) {
                        attributeInfos.add(new MBeanAttributeInfo(fieldName, List.class.getName(), fieldName, true, false, false));
                        converters.add(v -> JsonElement.getValuesAs((ArrayNode) v, JsonNode::textValue));
                    }
                }
                MBeanInfo mBeanInfo = new MBeanInfo("ObjectNodeDynamicMBean", "Remote JMX metrics", attributeInfos.toArray(new MBeanAttributeInfo[0]), new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
                ObjectName objectName = ObjectName.getInstance(name);
                managementServer.registerMBean(new ObjectNodeDynamicMBean(mBeanInfo, converters, metrics), objectName);
                polledMetrics.get(serverId).put(key, metrics);
            }

            return metrics;
        }

        @Override
        public void onComplete() {
            disruptor.shutdown();
            cleanup();
            logger.error("JMX publisher complete");
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("JMX publisher error", throwable);
            onComplete();
        }
    }
}
