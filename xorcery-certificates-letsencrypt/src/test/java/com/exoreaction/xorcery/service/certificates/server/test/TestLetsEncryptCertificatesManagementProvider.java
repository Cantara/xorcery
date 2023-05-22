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
package com.exoreaction.xorcery.service.certificates.server.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.certificates.CertificatesService;
import com.exoreaction.xorcery.service.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.List;

@Disabled
public class TestLetsEncryptCertificatesManagementProvider {

    int managerPort = Sockets.nextFreePort();
    String config = """
            instance.id: xorcery1
            instance.host: server1
            instance.domain: exoreaction.dev
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true    
            keystores.enabled: true
            dns.client.discovery.enabled: false
            certificates.subject: \"*.exoreaction.dev\" 
            """;

    @Test
    public void testCertificateRequest() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        StandardConfigurationBuilder configurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration1 = new Configuration.Builder()
                .with(configurationBuilder.addTestDefaultsWithYaml(config))
                .add("jetty.server.http.port", 80)
                .add("jetty.server.ssl.port", managerPort)
                .add("certificates.server.enabled", true)
                .build();
        System.out.println(configuration1);
        try (Xorcery xorcery = new Xorcery(configuration1)) {

            try {
                CertificatesService certificatesService = xorcery.getServiceLocator().getService(CertificatesService.class);

                List<X509Certificate> certificates = xorcery.getServiceLocator().getService(CertificatesProvider.class)
                        .requestCertificates(certificatesService.createRequest())
                        .toCompletableFuture().join();
                System.out.println(certificates);
            } catch (Exception e) {
                logger.error(e);
            }

/*
            System.out.println("Sleeping");
            Thread.sleep(60000);
*/
        }
    }
}
