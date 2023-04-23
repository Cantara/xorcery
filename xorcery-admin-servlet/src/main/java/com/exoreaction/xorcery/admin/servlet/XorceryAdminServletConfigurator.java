package com.exoreaction.xorcery.admin.servlet;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.exoreaction.xorcery.health.registry.XorceryHealthCheckService;
import com.exoreaction.xorcery.service.metricregistry.MetricRegistryWrapper;
import io.dropwizard.metrics.servlets.AdminServlet;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service
@RunLevel(15)
public class XorceryAdminServletConfigurator {

    private static final Logger logger = LogManager.getLogger(XorceryAdminServletConfigurator.class);

    @Inject
    public XorceryAdminServletConfigurator(
            @Named("root") MetricRegistryWrapper rootMetricRegistryWrapper,
            @Named("xorcery") MetricRegistryWrapper xorceryMetricRegistryWrapper,
            @Named("app") MetricRegistryWrapper appMetricRegistryWrapper,
            @Named("jvm") MetricRegistryWrapper jvmMetricRegistryWrapper,
            @Named("jersey") MetricRegistryWrapper jerseyMetricRegistryWrapper,
            @Named("jetty") MetricRegistryWrapper jettyMetricRegistryWrapper,
            XorceryHealthCheckService healthCheckService,
            XorceryVisualeCompatibleHealthServlet visualeCompatibleHealthServlet,
            ServletContextHandler servletContextHandler) {
        if (visualeCompatibleHealthServlet != null) {
            servletContextHandler.addServlet(new ServletHolder(visualeCompatibleHealthServlet), "/health/*");
            logger.info("Installed Visuale compatible health servlet at /health/*");
        }
        AdminServlet adminServlet = new AdminServlet();
        if (adminServlet != null) {
            if (rootMetricRegistryWrapper != null) {
                servletContextHandler.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, rootMetricRegistryWrapper.metricRegistry());
            }
            HealthCheckRegistry healthCheckRegistry = healthCheckService.codahaleRegistry();
            if (healthCheckRegistry != null) {
                servletContextHandler.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
            }
            servletContextHandler.addServlet(new ServletHolder(adminServlet), "/admin/*");
            logger.info("Installed admin servlet at /admin/*");
        }
        if (appMetricRegistryWrapper != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(appMetricRegistryWrapper.metricRegistry())), "/admin/metrics/app/*");
            logger.info("Installed app metrics at /admin/metrics/app/*");
        }
        if (xorceryMetricRegistryWrapper != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(xorceryMetricRegistryWrapper.metricRegistry())), "/admin/metrics/xorcery/*");
            logger.info("Installed xorcery metrics at /admin/metrics/xorcery/*");
        }
        if (jerseyMetricRegistryWrapper != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(jerseyMetricRegistryWrapper.metricRegistry())), "/admin/metrics/jersey/*");
            logger.info("Installed jersey metrics at /admin/metrics/jersey/*");
        }
        if (jettyMetricRegistryWrapper != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(jettyMetricRegistryWrapper.metricRegistry())), "/admin/metrics/jetty/*");
            logger.info("Installed jetty metrics at /admin/metrics/jetty/*");
        }
        if (jvmMetricRegistryWrapper != null) {
            servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(jvmMetricRegistryWrapper.metricRegistry())), "/admin/metrics/jvm/*");
            logger.info("Installed jvm metrics at /admin/metrics/jvm/*");
        }

    }
}
