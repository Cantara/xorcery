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
package com.exoreaction.xorcery.neo4j.browser;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Service(name = "neo4jbrowser")
@RunLevel(6)
public class Neo4jBrowser {

    @Inject
    public Neo4jBrowser(ServiceResourceObjects serviceResourceObjects,
                        Configuration configuration,
                        Provider<ServletContextHandler> ctxProvider) throws URISyntaxException {

        // Install servlet for Neo4j Browser
        URL webRootLocation = Resources.getResource("browser/index.html").orElseThrow();
        URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html", "/"));
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder servletHolder = new ServletHolder("neo4jbrowser", defaultServlet);
        ServletContextHandler servletContextHandler = ctxProvider.get();
        servletContextHandler.setInitParameter(DefaultServlet.CONTEXT_INIT + "resourceBase", webRootUri.toASCIIString());
        servletContextHandler.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", Boolean.TRUE.toString());
        servletContextHandler.addServlet(servletHolder, "/api/neo4j/browser/*");

        serviceResourceObjects.add(new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), "neo4jbrowser")
                .version(getClass().getPackage().getImplementationVersion())
                .api("browser", "api/neo4j/browser/")
                .build());
    }
}
