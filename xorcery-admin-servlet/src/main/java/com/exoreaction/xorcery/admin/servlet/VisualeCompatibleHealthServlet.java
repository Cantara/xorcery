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

import com.exoreaction.xorcery.health.registry.DefaultHealthCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

@Service
public class VisualeCompatibleHealthServlet extends HttpServlet {

    private static final long serialVersionUID = -7432916484889147321L;
    private static final Logger log = LogManager.getLogger(DefaultHealthCheckService.class);
    public static final String HEALTH_CHECK_REGISTRY = VisualeCompatibleHealthServlet.class.getCanonicalName() + ".registry";
    private transient DefaultHealthCheckService healthService;
    private transient ObjectMapper mapper;

    @Inject
    public VisualeCompatibleHealthServlet(DefaultHealthCheckService healthService) {
        this.healthService = healthService;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        Object executorAttr;
        if (null == this.healthService) {
            executorAttr = context.getAttribute(HEALTH_CHECK_REGISTRY);
            if (!(executorAttr instanceof DefaultHealthCheckService)) {
                throw new ServletException("Couldn't find a DefaultXorceryHealthCheckService instance.");
            }

            this.healthService = (DefaultHealthCheckService) executorAttr;
        }
        this.mapper = new ObjectMapper();
    }

    public void destroy() {
        super.destroy();
        this.healthService.shutdown();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ObjectNode health;
        try {
            health = healthService.getCurrentHealthJackson();
            long healthComputeTimeMs = healthService.getHealthComputeTimeMs();
            health.put("now", Instant.now().toString());
            health.put("health-compute-time-ms", String.valueOf(healthComputeTimeMs));
        } catch (Throwable t) {
            log.error("While getting health", t);
            health = mapper.createObjectNode();
            health.put("Status", "FAIL");
            health.put("errorMessage", "While getting health");
            StringWriter strWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(strWriter));
            health.put("errorCause", strWriter.toString());
        }
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        resp.setStatus(200);
        try (OutputStream output = resp.getOutputStream()) {
            try {
                this.getWriter(req).writeValue(output, health);
            } catch (Throwable t) {
                if (output != null) {
                    try {
                        output.close();
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                }
                throw t;
            }
        }
    }

    private ObjectWriter getWriter(HttpServletRequest request) {
        boolean prettyPrint = !Boolean.parseBoolean(request.getParameter("compact"));
        return prettyPrint ? this.mapper.writerWithDefaultPrettyPrinter() : this.mapper.writer();
    }
}