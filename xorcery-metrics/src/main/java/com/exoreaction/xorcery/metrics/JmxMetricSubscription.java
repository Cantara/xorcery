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
package com.exoreaction.xorcery.metrics;

import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

class JmxMetricSubscription
        implements Subscription {

    private final Logger logger = LogManager.getLogger(getClass());

    private final Subscriber<? super WithMetadata<ObjectNode>> subscriber;
    private DeploymentMetadata deploymentMetadata;
    private final Optional<List<String>> filters;
    private final MBeanServer managementServer;
    private final Set<MBeanAttributeInfo> unsupportedOperationAttributes = new HashSet<>();
    private final Map<ObjectName, Map<String, Function<Object, JsonNode>>> valueConverters = new HashMap<>();

    public JmxMetricSubscription(DeploymentMetadata deploymentMetadata, Subscriber<? super WithMetadata<ObjectNode>> subscriber, Optional<List<String>> filters, MBeanServer managementServer) {
        this.deploymentMetadata = deploymentMetadata;
        this.filters = filters;
        this.managementServer = managementServer;
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    private void addAttributes(MBeanServer managementServer, String filter, ObjectNode metricsBuilder) {
        try {
            ObjectName filterName = filter == null ? null : ObjectName.getInstance(filter);
            Set<ObjectName> objectNameSet = managementServer.queryNames(filterName, null);
            for (ObjectName objectName : objectNameSet) {

                if (objectName.getDomain().equals("JMImplementation") || objectName.getDomain().equals("remote"))
                    continue;

                Map<String, Function<Object, JsonNode>> converters = valueConverters.computeIfAbsent(objectName, on -> new HashMap<>());

                MBeanInfo mBeanInfo = managementServer.getMBeanInfo(objectName);

                ObjectNode mbeanMetricsBuilder = JsonNodeFactory.instance.objectNode();
                for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {

                    if (unsupportedOperationAttributes.contains(attribute))
                        continue;

                    try {
                        Object value = managementServer.getAttribute(objectName, attribute.getName());
                        if (value != null) {

                            Function<Object, JsonNode> converter = converters.get(attribute.getName());
                            if (converter == null) {
                                if (value instanceof String) {
                                    converter = o -> JsonNodeFactory.instance.textNode(o.toString());
                                } else if (value instanceof Long) {
                                    converter = o -> JsonNodeFactory.instance.numberNode((Long) o);
                                } else if (value instanceof Integer) {
                                    converter = o -> JsonNodeFactory.instance.numberNode((Integer) o);
                                } else if (value instanceof Double) {
                                    converter = o -> JsonNodeFactory.instance.numberNode((Double) o);
                                } else if (value instanceof Float) {
                                    converter = o -> JsonNodeFactory.instance.numberNode((Float) o);
                                } else if (value instanceof Boolean) {
                                    converter = o -> JsonNodeFactory.instance.booleanNode((Boolean) o);
                                } else if (value instanceof Map<?,?>) {
                                    converter = o ->
                                    {
                                        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                                        ((Map)o).forEach((k,v)->objectNode.set(k.toString(), JsonNodeFactory.instance.textNode(v.toString())));
                                        return objectNode;
                                    };
                                } else if (value instanceof ObjectName) {
                                    converter = o -> JsonNodeFactory.instance.textNode(o.toString());
                                } else if (value instanceof CompositeData cd) {
                                    String[] keys = cd.getCompositeType().keySet().toArray(new String[0]);
                                    converter = o ->
                                    {
                                        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                                        CompositeData compositeData = (CompositeData) o;
                                        Object[] values = compositeData.getAll(keys);
                                        for (int i = 0; i < keys.length; i++) {
                                            String key = keys[i];
                                            String compositeValue = values[i].toString();
                                            objectNode.set(key, objectNode.textNode(compositeValue));
                                        }
                                        return objectNode;
                                    };
                                } else if (value.getClass().isArray()) {
                                    converter = o ->
                                    {
                                        int length = Array.getLength(o);
                                        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(length);
                                        for(int i = 0; i < length; i++)
                                            arrayNode.add(Array.get(o, i).toString());
                                        return arrayNode;
                                    };
                                } else if (value instanceof List) {
                                    converter = o ->
                                    {
                                        List<Object> list = (List<Object>)o;
                                        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(list.size());
                                        for (Object v : list) {
                                            arrayNode.add(v.toString());
                                        }
                                        return arrayNode;
                                    };
                                } else {
                                    logger.debug("Unknown JMX attribute type:" + value.getClass().getName());
                                    converter = o -> JsonNodeFactory.instance.textNode(o.toString());
                                }

                                converters.put(attribute.getName(), converter);
                            }

                            // TODO Handle more types
                            mbeanMetricsBuilder.set(attribute.getName(), converter.apply(value));
                        }
                    } catch (RuntimeMBeanException e) {
                        if (e.getCause() instanceof UnsupportedOperationException) {
                            unsupportedOperationAttributes.add(attribute);
                        }
                    } catch (Throwable e) {
                        LogManager.getLogger(getClass()).error("Could not get metrics for {}: {}", objectName, attribute.getName(), e);
                        unsupportedOperationAttributes.add(attribute);
                    }
                }
                if (!mbeanMetricsBuilder.isEmpty()) {
                    metricsBuilder.set(objectName.getCanonicalName(), mbeanMetricsBuilder);
                }
            }
        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not get metrics", e);
        }
    }

    @Override
    public void request(long n) {

        try {
            ObjectNode metricsBuilder = JsonNodeFactory.instance.objectNode();
            filters.ifPresentOrElse(list ->
            {
                if (list.isEmpty()) {
                    addAttributes(managementServer, null, metricsBuilder);
                } else {
                    for (String filter : list) {
                        addAttributes(managementServer, filter, metricsBuilder);
                    }
                }
            }, () ->
            {
                addAttributes(managementServer, null, metricsBuilder);
            });

            for (long i = 0; i < n; i++) {
                subscriber.onNext(new WithMetadata<>(new Metadata.Builder(deploymentMetadata.context().metadata().deepCopy())
                        .add("timestamp", System.currentTimeMillis())
                        .build(), metricsBuilder));
            }

        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not send metrics", e);
        }
    }

    @Override
    public void cancel() {
        LogManager.getLogger(getClass()).info("Metrics subscription cancelled");
        subscriber.onComplete();
    }
}
