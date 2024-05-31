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
package com.exoreaction.xorcery.opentelemetry.jmx;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.lang.AutoCloseables;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SchemaUrls;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;

@Service(name = "opentelemetry.instrumentations.jvm")
@RunLevel(0)
public class OpenTelemetryJMXService
    implements PreDestroy
{

    private final MBeanServer managementServer;
    private final AutoCloseables closeables = new AutoCloseables();
    private final Logger logger;

    @Inject
    public OpenTelemetryJMXService(Configuration configuration, OpenTelemetry openTelemetry, Logger logger) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, IntrospectionException {
        this.logger = logger;

        this.managementServer = ManagementFactory.getPlatformMBeanServer();

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        OpenTelemetryJMXConfiguration jmxConfiguration = OpenTelemetryJMXConfiguration.get(configuration);
        Map<String, OpenTelemetryJMXConfiguration.JmxAttributeConfiguration> attributes = jmxConfiguration.getAttributes();
        logger.info(attributes);
        for (Map.Entry<String, OpenTelemetryJMXConfiguration.JmxAttributeConfiguration> entry : attributes.entrySet()) {
            OpenTelemetryJMXConfiguration.JmxAttributeConfiguration jmxAttributeConfiguration = entry.getValue();
            ObjectName objectName = ObjectName.getInstance(jmxAttributeConfiguration.getObjectName());
            switch (jmxAttributeConfiguration.getType()) {
                case "Gauge" -> {
                    MBeanInfo mBeanInfo = managementServer.getMBeanInfo(objectName);
                    DoubleGaugeBuilder doubleGaugeBuilder = meter.gaugeBuilder(entry.getKey())
                            .setUnit(jmxAttributeConfiguration.getUnit());
                    jmxAttributeConfiguration.getDescription().ifPresent(doubleGaugeBuilder::setDescription);
                    String attributeName = jmxAttributeConfiguration.getAttributeName();
                    Arrays.stream(mBeanInfo.getAttributes())
                            .filter(attr -> attr.getName().equals(attributeName))
                            .findFirst()
                            .ifPresentOrElse(attrInfo ->
                            {
                                switch (attrInfo.getType()) {
                                    case "double", "float" -> closeables.add(doubleGaugeBuilder.buildWithCallback(m ->
                                    {
                                        try {
                                            m.record(((Number) managementServer.getAttribute(objectName, attributeName)).doubleValue());
                                        } catch (Throwable t) {
                                            logger.error(String.format("Could not record JMX attribute %s %s", objectName, attributeName), t);
                                        }
                                    }));

                                    case "long", "int" -> closeables.add(doubleGaugeBuilder.ofLongs().buildWithCallback(m ->
                                    {
                                        try {
                                            m.record(((Number) managementServer.getAttribute(objectName, attributeName)).longValue());
                                        } catch (Throwable t) {
                                            logger.error(String.format("Could not record JMX attribute %s %s", objectName, attributeName), t);
                                        }
                                    }));

                                    default -> logger.error("Unknown JMX attribute type:{}", attrInfo.getType());
                                }
                            }, () -> logger.error("Unknown JMX attribute {} on objectName {}", attributeName, objectName.toString()));
                }

                default -> {
                    logger.error("Unknown JMX meter type:{}", jmxAttributeConfiguration.getType());
                }
            }
        }
    }

    @Override
    public void preDestroy() {
        try {
            closeables.close();
        } catch (Exception e) {
            logger.warn("Could not close metrics", e);
        }
    }
}
