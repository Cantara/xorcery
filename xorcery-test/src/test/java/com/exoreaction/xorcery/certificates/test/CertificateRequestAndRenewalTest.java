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
package com.exoreaction.xorcery.certificates.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.test.ClientTester;
import com.exoreaction.xorcery.net.Sockets;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;

@Disabled
public class CertificateRequestAndRenewalTest {

    int managerPort = Sockets.nextFreePort();
    String config = """
            dns.client.hosts:
                server1.xorcery.test: 127.0.0.1
                _certificates._sub._https._tcp: https://127.0.0.1:""" + managerPort +"""
            
            dns.server.enabled: false
            dns.client.discovery.enabled: false
            dns.registration.announce.enabled: false
            jetty.client:
                enabled: true
                ssl:
                    enabled: true
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true    
            """;

    @Test
    public void testCertificateRequestAndRenewal() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        StandardConfigurationBuilder configurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration1 = new Configuration.Builder()
                .with(configurationBuilder.addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server1.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", managerPort)
                .add("keystores.keystore.path", "META-INF/intermediatecakeystore.p12")
                .add("keystores.truststore.path", "META-INF/intermediatecatruststore.p12")
                .add("certificates.server.enabled", true)
                .add("certificates.enabled", true)
                .build();
        System.out.println(configuration1);
        Configuration configuration2 = new Configuration.Builder()
                .with(configurationBuilder.addTestDefaultsWithYaml(config))
                .add("id", "xorcery2")
                .add("host", "server2.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .add("keystores.keystore.path", "META-INF/keystore.p12")
                .add("keystores.truststore.path", "META-INF/truststore.p12")
                .add("certificates.client.enabled", true)
                .add("certificates.client.renewonstartup", true)
                .add("certificates.client.uri", "srv://_certificates")
                .build();
//        System.out.println(StandardConfigurationBuilder.toYaml(configuration2));
        try (Xorcery xorcery1 = new Xorcery(configuration1)) {
            try (Xorcery xorcery2 = new Xorcery(configuration2)) {
                System.out.println("DONE");
//                Thread.sleep(5000000);
            }
        }
    }

    @Test
    public void testCRL() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = 8443;
        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery1")
                .add("instance.host", "server.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", managerPort)
//                .add("server.ssl.enabled", false)
                .add("keystores.keystore.path", "META-INF/intermediatecakeystore.p12")
//                .add("keystores.keystore.path", "../xorcery-certificates-server/rootcakeystore.p12")
//                .add("certificates.server.alias", "root")
                .add("keystores.truststore.path", "META-INF/intermediatecatruststore.p12")
                .add("certificates.server.enabled", true)
                .add("certificates.enabled", true)
                .build();
        System.out.println(serverConfiguration);
        Configuration clientConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery2")
                .add("instance.host", "server2.xorcery.test")
                .add("clienttester.enabled", "true")
                .add("server.enabled", false)
                .add("server.enabled", false)
                .build();
        System.out.println(clientConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            try (Xorcery client = new Xorcery(clientConfiguration)) {
                System.out.println("DONE");

                InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
                URI crlUri = cfg.getAPI().resolve("certificates/crl");
                InputStream input = (InputStream) client.getServiceLocator().getService(ClientTester.class).get(UriBuilder.fromUri(crlUri).build()).get().getEntity();
                System.out.println(new String(input.readAllBytes()));
//                CRL crl = CertificateFactory.getInstance("X.509").generateCRL(input);
//                System.out.println(crl.toString());
//                Thread.sleep(5000000);
            }

        }
    }
}
