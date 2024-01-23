package com.exoreaction.xorcery.opentelemetry.jmx;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.InjectionPointIndicator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;

@Service(name = "opentelemetry.instrumentations.jvm")
@RunLevel(0)
public class OpenTelemetryJMXService {

    private final MBeanServer managementServer;

    @Inject
    public OpenTelemetryJMXService(Configuration configuration, OpenTelemetry openTelemetry, Logger logger) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, IntrospectionException {

        this.managementServer = ManagementFactory.getPlatformMBeanServer();

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
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
                                    case "double", "float" -> doubleGaugeBuilder.buildWithCallback(m ->
                                    {
                                        try {
                                            m.record(((Number) managementServer.getAttribute(objectName, attributeName)).doubleValue());
                                        } catch (Throwable t) {
                                            logger.error(String.format("Could not record JMX attribute %s %s", objectName, attributeName), t);
                                        }
                                    });

                                    case "long", "int" -> doubleGaugeBuilder.ofLongs().buildWithCallback(m ->
                                    {
                                        try {
                                            m.record(((Number) managementServer.getAttribute(objectName, attributeName)).longValue());
                                        } catch (Throwable t) {
                                            logger.error(String.format("Could not record JMX attribute %s %s", objectName, attributeName), t);
                                        }
                                    });

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
}
