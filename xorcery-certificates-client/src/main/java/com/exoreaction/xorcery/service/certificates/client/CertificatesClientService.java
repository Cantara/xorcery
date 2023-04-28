package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.jetty.client.HttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class CertificatesClientService
        implements AutoCloseable {

    private final Logger logger = LogManager.getLogger(getClass());
    private final String alias;

    private final KeyStores keyStores;
    private final Configuration configuration;
    private final JsonApiClient client;
    private final KeyStore keyStore;
    private final CertificatesClientConfiguration certificatesClientConfiguration;
    private CompletableFuture<X509CertificateHolder> result = new CompletableFuture<>();

    public CertificatesClientService(KeyStores keyStores,
                                     HttpClient httpClient,
                                     DnsLookupService dnsLookupService,
                                     Configuration configuration) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        this.keyStore = keyStores.getKeyStore("keystore");
        this.keyStores = keyStores;
        this.configuration = configuration;
        this.certificatesClientConfiguration = () -> configuration.getConfiguration("certificates.client");

        this.client = new JsonApiClient(httpClient, dnsLookupService);
        this.alias = configuration.getString("client.ssl.alias").orElse("self");

        // Request new certificate
        if (!keyStore.containsAlias(alias)) {
            // Check first that we have a provisioning key
            PrivateKey provisioning = (PrivateKey) keyStore.getKey("provisioning", "password".toCharArray());
            if (provisioning == null) {
                logger.error("Cannot provision certificate, missing 'provisioning' private key in keystore");
                return;
            }

            // Request certificate
            RequestCertificateProcess requestCertificateProcess = new RequestCertificateProcess(result, keyStore, keyStores, alias, certificatesClientConfiguration, configuration, client, logger);
            requestCertificateProcess.start();

            requestCertificateProcess.result().join();
        } else
        {
            // Renewal?
            if (certificatesClientConfiguration.isRenewOnStartup()) {
                RenewCertificateProcess renewCertificateProcess = new RenewCertificateProcess(result = new CompletableFuture<>(), keyStore, keyStores, alias, certificatesClientConfiguration, configuration, client, logger);
                renewCertificateProcess.start();
                renewCertificateProcess.result().join();
            }
        }

        scheduleRenewal();
    }

    private void scheduleRenewal() {
        try {
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            logger.info("Current certificate:" + certificate);
            Instant oneWeekBeforeExpiry = certificate.getNotAfter().toInstant().minus(7, ChronoUnit.DAYS);
            if (new Date().toInstant().isAfter(oneWeekBeforeExpiry)) {
                RenewCertificateProcess renewCertificateProcess = new RenewCertificateProcess(result = new CompletableFuture<>(), keyStore, keyStores, alias, certificatesClientConfiguration, configuration, client, logger);
                renewCertificateProcess.start();
                renewCertificateProcess.result().whenComplete((cert, throwable)->
                {
                    if (cert != null)
                    {
                        Instant newOneWeekBeforeExpiry = cert.getNotAfter().toInstant().minus(7, ChronoUnit.DAYS);
                        logger.info("Next certificate renewal scheduled to " + newOneWeekBeforeExpiry.toString());

                        CompletableFuture.delayedExecutor(newOneWeekBeforeExpiry.toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                                .execute(this::scheduleRenewal);
                    }
                });
            } else
            {
                logger.info("Next certificate renewal scheduled to " + oneWeekBeforeExpiry.toString());

                CompletableFuture.delayedExecutor(oneWeekBeforeExpiry.toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .execute(this::scheduleRenewal);

            }
        } catch (KeyStoreException e) {
            logger.error("Could not get certificate from keystore", e);
            result.completeExceptionally(e);
        }
    }

    @Override
    public void close() {
        if (result != null)
            result.cancel(true);
    }
}
