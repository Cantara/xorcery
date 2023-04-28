package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.process.Process;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public record RequestCertificateProcess(CompletableFuture<X509CertificateHolder> result, KeyStore keyStore,
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
                .map(uri -> requestCertificate(uri)
                        .thenRun(() ->
                        {
                            RenewCertificateProcess renewCertificateProcess = new RenewCertificateProcess(result, keyStore, keyStores, alias, certificatesClientConfiguration, configuration, client, logger);
                            renewCertificateProcess.start();
                        }))
                .ifPresent(CompletableFuture::join);
    }

    public CompletableFuture<X509CertificateHolder> requestCertificate(String certificatesUri) {
        return client.get(new Link("self", certificatesUri))
                .thenApply(ServerResourceDocument::new)
                .thenApply(this::getCertificatesRequest)
                .thenCompose(this::sendCertificateRequest)
                .whenComplete((rd, throwable) ->
                {
                    if (throwable != null) {
                        logger.error("Could not request certificate", throwable);
                        if (isRetryable(throwable))
                            retry();
                    } else {
                        succeeded();
                    }
                }).toCompletableFuture();
    }

    private Optional<Link> getCertificatesRequest(ServerResourceDocument certificatesHostApi) {
        return certificatesHostApi.getServiceByType("certificates").flatMap(sro ->
                sro.getLinkByRel("request"));
    }

    private CompletionStage<X509CertificateHolder> sendCertificateRequest(Optional<Link> requestLink) {
        return requestLink.map(link ->
        {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", PROVIDER_NAME);
                keyGen.initialize(256, new SecureRandom());
                KeyPair issuedCertKeyPair = keyGen.generateKeyPair();

                String csr = createRequest(issuedCertKeyPair);
                return client.submit(link, new ResourceObject.Builder("certificaterequest")
                                .attributes(new Attributes.Builder().attribute("csr", csr).build())
                                .build())
                        .thenApply(ServiceResourceObject::new)
                        .thenCompose(insertRequestedCertificate(issuedCertKeyPair));
            } catch (Throwable ex) {
                return CompletableFuture.<X509CertificateHolder>failedStage(ex);
            }
        }).orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("No request link in certificates API")));
    }

    private String createRequest(KeyPair issuedCertKeyPair) throws IOException, GeneralSecurityException, OperatorCreationException {
        X500Name issuedCertSubject = new X500Name("CN=server");

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic());

        // Sign the new KeyPair with the Provisioning cert Private Key
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC");
        PrivateKey provisioning = (PrivateKey) keyStore.getKey("provisioning", "password".toCharArray());
        if (provisioning == null) {
            throw new IOException("Missing 'provisioning' private key in keystore");
        }
        ContentSigner csrContentSigner = csrBuilder.build(provisioning);
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        // Convert to PEM format
        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE_REQUEST, csr.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }

    private Function<ServiceResourceObject,CompletionStage<X509CertificateHolder>> insertRequestedCertificate(KeyPair issuedCertKeyPair) {
        return sro ->
        {
            CompletableFuture<X509CertificateHolder> certFuture = new CompletableFuture<>();
            sro.getAttributes().attributes().getString("pem").ifPresent(certificatePem ->
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
                    keyStore.setKeyEntry(alias, issuedCertKeyPair.getPrivate(), password, chain.toArray(new java.security.cert.Certificate[0]));

                    // Remove provisioning cert
                    if (keyStore.containsAlias("provisioning"))
                        keyStore.deleteEntry("provisioning");

                    // Save it
                    keyStores.save(keyStore);

                    logger.info("Successfully provisioned service certificate");
                    certFuture.complete(certificate);
                } catch (Throwable e) {
                    throw new CompletionException("Could not update keystore with new certificate", e);
                }
            });
            if (!certFuture.isDone())
            {
                certFuture.complete(null);
            }
            return certFuture;
        };
    }
}
