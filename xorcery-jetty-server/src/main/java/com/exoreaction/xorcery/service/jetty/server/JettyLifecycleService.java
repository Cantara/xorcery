package com.exoreaction.xorcery.service.jetty.server;


import com.exoreaction.xorcery.health.api.XorceryHealthCheckRegistry;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckResult;
import com.exoreaction.xorcery.health.api.XorceryHealthStatus;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
@RunLevel(18)
public class JettyLifecycleService
        implements PreDestroy {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Server server;

    @Inject
    public JettyLifecycleService(Server server, XorceryHealthCheckRegistry healthCheckRegistry) throws Exception {
        this.server = server;
        healthCheckRegistry.register("jetty.server", this::check);
        server.start();
        logger.info("Started Jetty server");
    }

    @Override
    public void preDestroy() {
        logger.info("Stopping Jetty server");
        try {
            server.stop();
        } catch (Throwable e) {
            logger.error(e);
        }
    }

    XorceryHealthCheckResult check() {
        ObjectNode info = JsonNodeFactory.instance.objectNode();
        String lifeCycleState = toLifeCycleState(server);
        info.put("lifecycle", lifeCycleState);
        ArrayNode connectors = info.putArray("connectors");
        for (Connector connector : server.getConnectors()) {
            ObjectNode connectorInfo = JsonNodeFactory.instance.objectNode();
            if (connector instanceof NetworkConnector networkConnector) {
                connectorInfo.put("host", networkConnector.getHost());
                connectorInfo.put("port", networkConnector.getPort());
                connectorInfo.put("localPort", networkConnector.getLocalPort());
            }
            connectorInfo.put("name", connector.getName());
            connectorInfo.put("protocols", String.join(", ", connector.getProtocols()));
            connectorInfo.put("idleTimeout", connector.getIdleTimeout());
            connectorInfo.put("lifecycle", toLifeCycleState(connector));
            connectors.add(connectorInfo);
        }
        ObjectNode threadpoolNode = info.putObject("threadpool");
        ThreadPool threadPool = server.getThreadPool();
        threadpoolNode.put("threads", threadPool.getThreads());
        threadpoolNode.put("idleThreads", threadPool.getIdleThreads());
        XorceryHealthStatus healthStatus = toHealthStatus(server);
        return new XorceryHealthCheckResult(healthStatus, healthStatus.healthy() ? null : lifeCycleState, null, info);
    }

    private static String toLifeCycleState(LifeCycle lifeCycle) {
        if (lifeCycle.isRunning()) {
            return "running";
        }
        if (lifeCycle.isStarted()) {
            return "started";
        }
        if (lifeCycle.isFailed()) {
            return "failed";
        }
        if (lifeCycle.isStopped()) {
            return "stopped";
        }
        if (lifeCycle.isStarting()) {
            return "starting";
        }
        if (lifeCycle.isStopping()) {
            return "stopping";
        }
        return "unknown";
    }

    private static XorceryHealthStatus toHealthStatus(LifeCycle lifeCycle) {
        if (lifeCycle.isRunning()) {
            return XorceryHealthStatus.HEALTHY;
        }
        if (lifeCycle.isStarting() || lifeCycle.isStarted()) {
            return XorceryHealthStatus.INITIALIZING;
        }
        return XorceryHealthStatus.UNHEALTHY;
    }
}
