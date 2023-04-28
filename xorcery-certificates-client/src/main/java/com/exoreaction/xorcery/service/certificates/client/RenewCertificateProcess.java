package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.process.Process;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.StringReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
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

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public record RenewCertificateProcess(CompletableFuture<X509CertificateHolder> result, KeyStore keyStore,
                                      KeyStores keyStores,
                                      String alias,
                                      CertificatesClientConfiguration certificatesClientConfiguration,
                                      Configuration configuration,
                                      JsonApiClient client,
                                      Logger logger)
        implements Process<X509CertificateHolder> {
    @Override
    public void start() {

        certificatesClientConfiguration.getURI()
                .ifPresentOrElse(uri -> renewCertificate(uri).whenComplete(this::complete),
                        () -> result.complete(null));
    }

    public CompletionStage<X509CertificateHolder> renewCertificate(String certificatesHost) {
        return client.get(new Link("self", certificatesHost))
                .thenApply(ServerResourceDocument::new)
                .thenApply(this::getCertificatesRenewal)
                .thenCompose(this::sendCertificateRenewal);
    }

    private Optional<Link> getCertificatesRenewal(ServerResourceDocument certificatesHostApi) {
        return certificatesHostApi.getServiceByType("certificates").flatMap(sro ->
                sro.getLinkByRel("renewal"));
    }

    private CompletionStage<X509CertificateHolder> sendCertificateRenewal(Optional<Link> requestLink) {
        return requestLink.map(link ->
        {
            try {
                return client.get(link).thenCompose(this::insertRenewedCertificate);
            } catch (Throwable ex) {
                return CompletableFuture.<X509CertificateHolder>failedStage(ex);
            }
        }).orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("No request link in certificates API")));
    }

    private CompletionStage<X509CertificateHolder> insertRenewedCertificate(ResourceDocument rd) {
        CompletableFuture<X509CertificateHolder> certFuture = new CompletableFuture<>();
        rd.getResources().ifPresent(ros ->
        {
            ros.forEach(ro ->
            {
                ro.getAttributes().getString("pem").ifPresent(certificatePem ->
                {
                    try {
                        // Update keystore
                        List<X509Certificate> chain = new ArrayList<>();
                        PEMParser pemParser = new PEMParser(new StringReader(certificatePem));
                        X509CertificateHolder certificate;
                        while ((certificate = (X509CertificateHolder) pemParser.readObject()) != null) {
                            chain.add(new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificate));
                        }

                        char[] password = configuration.getString("keystores.keystore.password").map(String::toCharArray).orElse(null);
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
                        keyStore.setKeyEntry(alias, privateKey, password, chain.toArray(new java.security.cert.Certificate[0]));

                        // Save it
                        keyStores.save(keyStore);

                        logger.info("Successfully renewed service certificate");
                        certFuture.complete(certificate);
                    } catch (Throwable e) {
                        throw new CompletionException("Could not update keystore with renewed certificate", e);
                    }
                });
            });
        });
        if (!certFuture.isDone())
            certFuture.complete(null);
        return certFuture;
    }
}
