package com.exoreaction.xorcery.admin.servlet;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.exoreaction.xorcery.health.registry.HealthCheckService;
import io.dropwizard.metrics.servlets.AdminServlet;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service
@RunLevel(20)
public class AdminServletConfigurator {

    private static final Logger logger = LogManager.getLogger(AdminServletConfigurator.class);

    @Inject
    public AdminServletConfigurator(
            MetricRegistry metricRegistry,
            HealthCheckService healthCheckService,
            VisualeCompatibleHealthServlet visualeCompatibleHealthServlet,
            ServletContextHandler servletContextHandler) {
        if (visualeCompatibleHealthServlet != null) {
            servletContextHandler.addServlet(new ServletHolder(visualeCompatibleHealthServlet), "/health/*");
            logger.info("Installed Visuale compatible health servlet at /health/*");
        }
        AdminServlet adminServlet = new AdminServlet();
        servletContextHandler.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        HealthCheckRegistry healthCheckRegistry = healthCheckService.codahaleRegistry();
        if (healthCheckRegistry != null) {
            servletContextHandler.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
        }
        servletContextHandler.addServlet(new ServletHolder(adminServlet), "/admin/*");
        logger.info("Installed admin servlet at /admin/*");

        servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(metricRegistry)), "/admin/metrics/*");
        logger.info("Installed metrics at /admin/metrics");
    }
}
