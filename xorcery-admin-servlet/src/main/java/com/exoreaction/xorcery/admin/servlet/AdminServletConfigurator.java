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

@Service(name="adminservlet")
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
