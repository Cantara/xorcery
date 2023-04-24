package com.exoreaction.xorcery.health.registry;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.exoreaction.xorcery.health.api.XorceryHealthCheck;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckRegistry;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DefaultXorceryHealthCheckService implements XorceryHealthCheckService {

    private static final Logger log = LogManager.getLogger(DefaultXorceryHealthCheckService.class);

    private final Map<String, XorceryHealthCheck> healthCheckByComponentName = new ConcurrentHashMap<>();

    /*
     * Thread-safe state
     */
    private final String timeAtStart = Instant.now().toString();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);
    private final long minUpdateInterval;
    private final TemporalUnit updateIntervalUnit;
    private final AtomicLong healthComputeTimeMs = new AtomicLong(-1);
    private final HealthCheckRegistry healthCheckRegistry;
    private final List<XorceryHealthProbe> healthProbes = new CopyOnWriteArrayList<>();

    /*
     * State only that is only read and written by the healthUpdateThread, so no need for synchronization
     */
    private final Object lock = new Object();
    private long lastUpdatedEpoch = 0;
    ObjectNode currentHealth;

    public DefaultXorceryHealthCheckService(String version, String ip, String ipAll, HealthCheckRegistry healthCheckRegistry, long minUpdateInterval, TemporalUnit updateIntervalUnit) {
        this.minUpdateInterval = minUpdateInterval;
        this.updateIntervalUnit = updateIntervalUnit;
        this.healthCheckRegistry = healthCheckRegistry;
        try {
            synchronized (lock) {
                currentHealth = mapper.createObjectNode();
                currentHealth.put("Status", "false");
                currentHealth.put("version", version);
                currentHealth.put("ip", ip);
                currentHealth.put("ip-all", ipAll);
                currentHealth.put("running since", timeAtStart);
                // TODO add service-name / alias / context-path and some other basic info/config about app
            }
        } catch (Throwable t) {
            log.warn("While setting health initialization message", t);
        }
    }

    @Override
    public HealthCheckRegistry codahaleRegistry() {
        return healthCheckRegistry;
    }

    public DefaultXorceryHealthCheckService registerHealthCheck(String key, HealthCheck healthCheck) {
        healthCheckRegistry.register(key, healthCheck);
        healthCheckByComponentName.put(key, new WrappedHealthCheck(mapper, healthCheck));
        return this;
    }

    @Override
    public DefaultXorceryHealthCheckService register(String componentName, XorceryHealthCheck healthCheck) {
        healthCheckByComponentName.put(componentName, healthCheck);
        healthCheckRegistry.register(componentName, new HealthCheckAdapter(healthCheck));
        return this;
    }

    @Override
    public XorceryHealthCheckRegistry registerHealthProbe(String key, Supplier<Object> probe) {
        return null;
    }

    @Override
    public SortedMap<String, XorceryHealthCheckResult> runHealthChecks() {
        SortedMap<String, XorceryHealthCheckResult> result = new TreeMap<>();
        for (Map.Entry<String, XorceryHealthCheck> entry : healthCheckByComponentName.entrySet()) {
            String componentName = entry.getKey();
            XorceryHealthCheck healthCheck = entry.getValue();
            XorceryHealthCheckResult healthCheckResult = healthCheck.check();
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
        long minimumUpdateWaitTimeMs = Duration.of(minUpdateInterval, updateIntervalUnit).toMillis();
        if (minimumUpdateWaitTimeMs < millisSinceLastUpdate) {
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
        SortedMap<String, XorceryHealthCheckResult> healthCheckResultByKey = runHealthChecks();
        for (Map.Entry<String, XorceryHealthCheckResult> entry : healthCheckResultByKey.entrySet()) {
            String key = entry.getKey();
            XorceryHealthCheckResult result = entry.getValue();
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
        for (XorceryHealthProbe healthProbe : healthProbes) {
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

    public static String getMyIPAddresssesString() {
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
        String fullString = getMyIPAddresssesString();
        String[] parts = fullString.split("\\s");
        if (parts.length == 0) {
            return "";
        }
        return parts[0];
    }

    public static String readMetaInfMavenPomVersion(String groupId, String artifactId) {
        String resourcePath = String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
        URL mavenVersionResource = DefaultXorceryHealthCheckService.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                Properties mavenProperties = new Properties();
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "unknown";
    }
}
