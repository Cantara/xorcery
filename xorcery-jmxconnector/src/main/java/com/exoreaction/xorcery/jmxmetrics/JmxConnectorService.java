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
package com.exoreaction.xorcery.jmxmetrics;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

/**
 * JMX RMI Connector manager
 *
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = "jmxconnector")
@RunLevel(6)
public class JmxConnectorService
        implements PreDestroy {

    private final JMXConnectorServer jmxConnectorServer;
    private final Logger logger;

    @Inject
    public JmxConnectorService(Configuration configuration, Secrets secrets, Logger logger) throws IOException {
        this.logger = logger;

        JmxConnectorConfiguration jmxConnectorConfiguration = new JmxConnectorConfiguration(configuration.getConfiguration("jmxconnector"));

        if (jmxConnectorConfiguration.isSslEnabled())
        {
            // TODO This needs to be hooked up with keystores
            SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(jmxConnectorConfiguration.getRegistryPort(), csf, ssf);
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry(jmxConnectorConfiguration.getRegistryPort());
            }

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JMXServiceURL url = new JMXServiceURL(jmxConnectorConfiguration.getURI());

            Map<String, ?> environment = Map.of(JMXConnectorServer.AUTHENTICATOR, new ConfigJmxAuthenticator(jmxConnectorConfiguration.getUsers(), secrets),
                    RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf,
                    RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf,
                    "com.sun.jndi.rmi.factory.socket", csf);
            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, environment, mbs);
            jmxConnectorServer.start();
        } else
        {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(jmxConnectorConfiguration.getRegistryPort());
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry(jmxConnectorConfiguration.getRegistryPort());
            }

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            JMXServiceURL url = new JMXServiceURL(jmxConnectorConfiguration.getURI());

            Map<String, ?> environment = Map.of(JMXConnectorServer.AUTHENTICATOR, new ConfigJmxAuthenticator(jmxConnectorConfiguration.getUsers(), secrets));
            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, environment, mbs);
            jmxConnectorServer.start();
        }
        logger.info("JMX connector available at: {}", jmxConnectorConfiguration.getExternalURI());
    }

    @Override
    public void preDestroy() {
        try {
            jmxConnectorServer.stop();
        } catch (IOException e) {
            logger.warn("Could not stop JMX connector", e);
        }
    }
}
