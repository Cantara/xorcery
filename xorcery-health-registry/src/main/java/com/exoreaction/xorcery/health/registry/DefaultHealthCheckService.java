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
package com.exoreaction.xorcery.health.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.health.api.HealthCheck;
import com.exoreaction.xorcery.health.api.HealthCheckRegistry;
import com.exoreaction.xorcery.health.api.HealthCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DefaultHealthCheckService implements HealthCheckService {

    private static final Logger log = LogManager.getLogger(DefaultHealthCheckService.class);

    private final Map<String, HealthCheck> healthCheckByComponentName = new ConcurrentHashMap<>();

    /*
     * Thread-safe state
     */
    private final String timeAtStart = Instant.now().toString();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);
    private final long minUpdateInterval;
    private final AtomicLong healthComputeTimeMs = new AtomicLong(-1);
    private final com.codahale.metrics.health.HealthCheckRegistry healthCheckRegistry;
    private final List<HealthProbe> healthProbes = new CopyOnWriteArrayList<>();

    /*
     * State only that is only read and written by the healthUpdateThread, so no need for synchronization
     */
    private final Object lock = new Object();
    private long lastUpdatedEpoch = 0;
    ObjectNode currentHealth;

    public DefaultHealthCheckService(Configuration configuration, com.codahale.metrics.health.HealthCheckRegistry healthCheckRegistry) {
        this.minUpdateInterval = Duration.parse("PT"+configuration.getString("health.updater.interval").orElse("1S")).toMillis();
        this.healthCheckRegistry = healthCheckRegistry;
        InstanceConfiguration instanceConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        try {
            synchronized (lock) {
                currentHealth = mapper.createObjectNode();
                currentHealth.put("Status", "false");
                currentHealth.put("version", instanceConfiguration.getVersion());
                currentHealth.put("name", instanceConfiguration.getName());
                currentHealth.put("ip", instanceConfiguration.getIp().getHostAddress());
                currentHealth.put("ip-all", getMyIPAddressesString());
                currentHealth.put("running since", timeAtStart);
                // TODO add service-name / alias / context-path and some other basic info/config about app
            }
        } catch (Throwable t) {
            log.warn("While setting health initialization message", t);
        }
    }

    @Override
    public com.codahale.metrics.health.HealthCheckRegistry codahaleRegistry() {
        return healthCheckRegistry;
    }

    public DefaultHealthCheckService registerHealthCheck(String key, com.codahale.metrics.health.HealthCheck healthCheck) {
        healthCheckRegistry.register(key, healthCheck);
        healthCheckByComponentName.put(key, new WrappedHealthCheck(mapper, healthCheck));
        return this;
    }

    @Override
    public DefaultHealthCheckService register(String componentName, HealthCheck healthCheck) {
        healthCheckByComponentName.put(componentName, healthCheck);
        healthCheckRegistry.register(componentName, new HealthCheckAdapter(healthCheck));
        return this;
    }

    @Override
    public HealthCheckRegistry registerHealthProbe(String key, Supplier<Object> probe) {
        return null;
    }

    @Override
    public SortedMap<String, HealthCheckResult> runHealthChecks() {
        SortedMap<String, HealthCheckResult> result = new TreeMap<>();
        for (Map.Entry<String, HealthCheck> entry : healthCheckByComponentName.entrySet()) {
            String componentName = entry.getKey();
            HealthCheck healthCheck = entry.getValue();
            HealthCheckResult healthCheckResult = healthCheck.check();
            result.put(componentName, healthCheckResult);
        }
        return result;
    }

    public String getCurrentHealthJson() {
        synchronized (lock) {
            updateIfNotSpammed();
            return currentHealth.toPrettyString();
        }
    }

    public ObjectNode getCurrentHealthJackson() {
        synchronized (lock) {
            updateIfNotSpammed();
            return currentHealth.deepCopy();
        }
    }

    private void updateIfNotSpammed() {
        long now = System.currentTimeMillis();
        long millisSinceLastUpdate = now - lastUpdatedEpoch;
        if (minUpdateInterval < millisSinceLastUpdate) {
            performHealthUpdate();
            lastUpdatedEpoch = now;
        }
    }

    public long getHealthComputeTimeMs() {
        return healthComputeTimeMs.get();
    }

    public void shutdown() {
        shouldRun.set(false);
    }

    private void performHealthUpdate() {
        try {
            boolean changed = updateHealth(currentHealth);
        } catch (Throwable t) {
            log.error("While updating health", t);
            {
                ObjectNode health = mapper.createObjectNode();
                health.put("Status", "FAIL");
                health.put("errorMessage", "Exception while updating health");
                StringWriter strWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(strWriter));
                health.put("errorCause", strWriter.toString());
                currentHealth = health;
            }
        }
    }

    private boolean updateHealth(ObjectNode health) {
        long start = System.currentTimeMillis();
        boolean changed = false;
        boolean status = true; // healthy
        SortedMap<String, HealthCheckResult> healthCheckResultByKey = runHealthChecks();
        for (Map.Entry<String, HealthCheckResult> entry : healthCheckResultByKey.entrySet()) {
            String key = entry.getKey();
            HealthCheckResult result = entry.getValue();
            boolean healthy = result.status().healthy();
            status &= healthy; // all health-checks must be healthy in order for status to be true
            ObjectNode field = (ObjectNode) health.get(key);
            if (field == null) {
                field = health.putObject(key);
            }
            changed |= updateField(field, "status", () -> healthy ? "UP" : "DOWN");
            JsonNode existingDetails = field.get("details");
            if (existingDetails == null) {
                if (result.info() != null) {
                    changed = true;
                    field.set("details", result.info());
                }
            } else {
                boolean infoChanged = !existingDetails.equals(result.info());
                changed |= infoChanged;
                if (infoChanged) {
                    field.set("details", result.info());
                }
            }
        }
        boolean effectiveStatus = status;
        changed |= updateField(health, "Status", () -> effectiveStatus ? "UP" : "DOWN");
        for (HealthProbe healthProbe : healthProbes) {
            changed |= updateField(health, healthProbe.key, healthProbe.probe);
        }
        long end = System.currentTimeMillis();
        healthComputeTimeMs.set(end - start);
        return changed;
    }

    private boolean updateField(ObjectNode health, String key, Supplier<Object> valueConsumer) {
        Object value = null;
        try {
            value = valueConsumer.get();
        } catch (Throwable t) {
            log.warn(String.format("Ignoring health field, error while attempting to compute field: '%s'", key), t);
        }
        if (value == null) {
            return updateField(health, key, (String) null);
        }
        if (value instanceof String) {
            return updateField(health, key, (String) value);
        }
        if (value instanceof JsonNode) {
            return updateField(health, key, (JsonNode) value);
        }
        return updateField(health, key, String.valueOf(value));
    }

    private boolean updateField(ObjectNode health, String key, String value) {
        JsonNode field = health.get(key);
        if (value == null) {
            if (field == null) {
                return false;
            }
            health.remove(key);
            return true;
        }
        if (field != null && !field.isNull() && field.isTextual() && field.textValue().equals(value)) {
            return false;
        }
        health.put(key, value);
        return true;
    }

    private boolean updateField(ObjectNode health, String key, JsonNode value) {
        JsonNode field = health.get(key);
        if (value == null) {
            if (field == null) {
                return false;
            }
            health.remove(key);
            return true;
        }
        if (field != null && field.equals(value)) {
            return false;
        }
        health.set(key, value);
        return true;
    }

    public static String getMyIPAddressesString() {
        List<Inet4Address> ip4loopBackIpAddresses = new ArrayList<>();
        List<Inet6Address> ip6loopBackIpAddresses = new ArrayList<>();
        List<Inet4Address> ip4siteLocalAddresses = new ArrayList<>();
        List<Inet6Address> ip6siteLocalAddresses = new ArrayList<>();

        try {
            Enumeration n = NetworkInterface.getNetworkInterfaces();

            while (n.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) n.nextElement();

                for (Enumeration a = networkInterface.getInetAddresses(); a.hasMoreElements(); ) {
                    InetAddress addr = (InetAddress) a.nextElement();
                    boolean loopbackAddress = addr.isLoopbackAddress();
                    boolean siteLocalAddress = addr.isSiteLocalAddress();
                    if (addr instanceof Inet4Address ip4addr) {
                        if (siteLocalAddress) {
                            ip4siteLocalAddresses.add(ip4addr);
                        } else if (loopbackAddress) {
                            ip4loopBackIpAddresses.add(ip4addr);
                        }
                    } else if (addr instanceof Inet6Address ip6addr) {
                        if (siteLocalAddress) {
                            ip6siteLocalAddresses.add(ip6addr);
                        } else if (loopbackAddress) {
                            ip6loopBackIpAddresses.add(ip6addr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "Not resolved";
        }

        return String.join("  ", Stream.of(ip4siteLocalAddresses, ip4loopBackIpAddresses, ip6siteLocalAddresses, ip6loopBackIpAddresses)
                .flatMap(Collection::stream)
                .map(InetAddress::getHostAddress)
                .toList()
        );
    }

    public static String getMyIPAddresssString() {
        String fullString = getMyIPAddressesString();
        String[] parts = fullString.split("\\s");
        if (parts.length == 0) {
            return "";
        }
        return parts[0];
    }
}
