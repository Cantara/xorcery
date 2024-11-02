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
package dev.xorcery.certificates.client;

import dev.xorcery.certificates.provisioning.CertificateProvisioningService;
import dev.xorcery.certificates.spi.CertificatesProvider;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.List;

@Disabled
public class CertificateRequestTest {

    Logger LOG = LogManager.getLogger(getClass());

    String config = """
            instance.id: xorcery1
            instance.host: server1
            instance.domain: exoreaction.dev
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true
            jetty.server.ssl.port: "{{ CALCULATED.dynamicPorts.ssl }}"    
            keystores.enabled: true
            dns.client.discovery.enabled: false
            certificates.server.enabled: true
            certificates.client.enabled: true
            certificates.client.uri: http://localhost:80
            certificates.subject: \"*.exoreaction.dev\"
            intermediateca.enabled: true 
            """;

    @Test
    public void testCertificateRequest() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        Configuration configuration1 = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();

        System.out.println(configuration1);
        try (Xorcery xorcery = new Xorcery(configuration1)) {

            try {
                CertificateProvisioningService certificatesService = xorcery.getServiceLocator().getService(CertificateProvisioningService.class);

                CertificatesProvider service = xorcery.getServiceLocator().getService(CertificatesProvider.class);
                List<X509Certificate> certificates = service
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
