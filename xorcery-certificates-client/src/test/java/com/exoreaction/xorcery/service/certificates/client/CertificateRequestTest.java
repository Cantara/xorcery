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
package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.certificates.CertificatesService;
import com.exoreaction.xorcery.service.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

@Disabled
public class CertificateRequestTest {

    Logger LOG = LogManager.getLogger(getClass());

    int managerPort = Sockets.nextFreePort();
    String config = """
            instance.id: xorcery1
            instance.host: server1
            instance.domain: exoreaction.dev
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true    
            keystores.enabled: true
            dns.client.discovery.enabled: false
            certificates.client.enabled: true
            certificates.client.uri: http://localhost:80
            certificates.subject: \"*.exoreaction.dev\"
            intermediateca.enabled: true 
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
