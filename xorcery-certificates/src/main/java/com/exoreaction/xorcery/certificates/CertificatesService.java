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
package com.exoreaction.xorcery.certificates;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.keystores.KeyStores;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class CertificatesService
        implements AutoCloseable {

    private final Logger logger = LogManager.getLogger(getClass());
    private final String alias;

    private final RequestCertificateProcess.Factory requestCertificatesProcessFactory;
    private final KeyStore keyStore;
    private final KeyPair issuedCertKeyPair;
    private final KeyStores keyStores;
    private final CertificatesConfiguration certificatesConfiguration;
    private final char[] password;
    private RequestCertificateProcess requestCertificateProcess;

    public CertificatesService(RequestCertificateProcess.Factory requestCertificateProcessFactory,
                               KeyStores keyStores,
                               Configuration configuration) throws GeneralSecurityException, IOException, OperatorCreationException {
        this.requestCertificatesProcessFactory = requestCertificateProcessFactory;
        this.keyStore = keyStores.getKeyStore("keystore");
        this.keyStores = keyStores;
        certificatesConfiguration = () -> configuration.getConfiguration("certificates");

        this.alias = certificatesConfiguration.getAlias();
        password = configuration.getString("keystores.keystore.password").map(String::toCharArray).orElse(null);

        // Create private key
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", PROVIDER_NAME);
        keyGen.initialize(256, new SecureRandom());
        issuedCertKeyPair = keyGen.generateKeyPair();

        // Request new certificate
        if (!keyStore.containsAlias(alias) || certificatesConfiguration.isRenewOnStartup()) {
            // Request certificate
            requestCertificateProcess = requestCertificatesProcessFactory.create(createRequest());
            requestCertificateProcess.start();
            requestCertificateProcess.result()
                    .whenComplete(this::updateCertificates).join();
        }

        scheduleRenewal();
    }

    public PKCS10CertificationRequest createRequest() throws IOException, GeneralSecurityException, OperatorCreationException {
        X500Name issuedCertSubject = new X500NameBuilder()
                .addRDN(BCStyle.CN, certificatesConfiguration.getSubject())
                .build();

        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
        extensionsGenerator.addExtension(
                Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        extensionsGenerator.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(
                        new KeyPurposeId[]{
                                KeyPurposeId.id_kp_clientAuth,
                                KeyPurposeId.id_kp_serverAuth}
                ));

        List<GeneralName> subAtlNamesList = new ArrayList<>();
        certificatesConfiguration.getIpAddresses().forEach(ipAddress -> subAtlNamesList.add(new GeneralName(GeneralName.iPAddress, ipAddress)));
        certificatesConfiguration.getDnsNames().forEach(dnsName -> subAtlNamesList.add(new GeneralName(GeneralName.dNSName, dnsName)));
        GeneralNames subAtlNames = new GeneralNames(subAtlNamesList.toArray(new GeneralName[0]));
        extensionsGenerator.addExtension(
                Extension.subjectAlternativeName, true, subAtlNames);

        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic())
                .addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensionsGenerator.generate());

        // Sign the new KeyPair with the Provisioning cert Private Key
        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC");
        PrivateKey provisioning = (PrivateKey) keyStore.getKey("provisioning", "password".toCharArray());
        logger.warn("No provisioning key in keystore. Using self-signing key instead");

        PrivateKey signingKey = provisioning != null ? provisioning : issuedCertKeyPair.getPrivate();

        ContentSigner csrContentSigner = contentSignerBuilder.build(signingKey);
        PKCS10CertificationRequest csr = csrBuilder.build(csrContentSigner);

        return csr;
    }

    public void scheduleRenewal() {
        try {
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            long earliestExpiry = certificate.getNotAfter().getTime();
            int i = 1;
            while (keyStore.containsAlias(alias + i)) {
                X509Certificate otherCert = (X509Certificate) keyStore.getCertificate(alias + i);
                logger.info("Current certificate:" + otherCert);
                earliestExpiry = Math.min(earliestExpiry, otherCert.getNotAfter().getTime());
                i++;
            }
            Instant oneWeekBeforeExpiry = Instant.ofEpochMilli(earliestExpiry).minus(7, ChronoUnit.DAYS);

            logger.info("Current certificate:" + certificate);
            if (new Date().toInstant().isAfter(oneWeekBeforeExpiry)) {
                requestCertificateProcess = requestCertificatesProcessFactory.create(createRequest());
                requestCertificateProcess.start();
                requestCertificateProcess.result()
                        .whenComplete(this::updateCertificates)
                        .whenComplete((certs, throwable) ->
                        {
                            scheduleRenewal();
                        });
            } else {
                logger.info("Next certificate renewal scheduled to " + oneWeekBeforeExpiry);

                CompletableFuture.delayedExecutor(oneWeekBeforeExpiry.toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .execute(this::scheduleRenewal);

            }
        } catch (IOException | GeneralSecurityException | OperatorCreationException e) {
            logger.error("Could not get certificate from keystore", e);
        }
    }

    @Override
    public void close() {
        if (requestCertificateProcess != null) {
            requestCertificateProcess.stop();
            requestCertificateProcess.result().join();
        }
    }

    private void updateCertificates(List<X509Certificate> certificates, Throwable throwable) {

        try {
            if (throwable != null)
                throw throwable;

            // If we have cert chains from many providers, split them up
            List<X509Certificate> chain = new ArrayList<>();
            int i = 1;
            for (X509Certificate certificate : certificates) {
                if (certificate.getBasicConstraints() == -1 && !chain.isEmpty()) {
                    String certAlias = alias + i;
                    keyStore.setKeyEntry(certAlias, issuedCertKeyPair.getPrivate(), password, chain.toArray(new java.security.cert.Certificate[0]));
                    chain.clear();
                    i++;
                }
                chain.add(certificate);
            }

            // Insert main chain
            keyStore.setKeyEntry(alias, issuedCertKeyPair.getPrivate(), password, chain.toArray(new java.security.cert.Certificate[0]));

            // Remove any trailing chains from previous runs
            while (keyStore.containsAlias(alias + i)) {
                keyStore.deleteEntry(alias + i);
                i++;
            }

            // Save it
            keyStores.save(keyStore);

            logger.info("Successfully requested certificate");
        } catch (Throwable e) {
            throw new CompletionException("Could not update keystore with new certificate", e);
        }
    }

}
