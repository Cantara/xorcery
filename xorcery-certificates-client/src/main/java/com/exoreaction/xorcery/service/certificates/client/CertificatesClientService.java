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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class CertificatesClientService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final String alias;

    private final KeyStores keyStores;
    private final Configuration configuration;
    private final JsonApiClient client;
    private final KeyStore keyStore;

    public CertificatesClientService(KeyStores keyStores,
                                     HttpClient httpClient,
                                     DnsLookupService dnsLookupService,
                                     Configuration configuration) throws KeyStoreException {
        this.keyStore = keyStores.getKeyStore("keystores.keystore");
        this.keyStores = keyStores;
        this.configuration = configuration;
        this.alias = configuration.getString("client.ssl.alias").orElse("self");

        this.client = new JsonApiClient(httpClient, dnsLookupService);

        if (keyStore.containsAlias(alias)) {
            // Renewal?
            configuration.getString("certificates.client.uri")
                    .map(this::renewCertificate)
                    .ifPresent(CompletableFuture::join);

        } else {
            // Request
            configuration.getString("certificates.client.uri")
                    .map(this::requestCertificate)
                    .ifPresent(CompletableFuture::join);
        }
    }

    public CompletableFuture<Void> requestCertificate(String certificatesUri) {
        return client.get(new Link("self", certificatesUri))
                .thenApply(ServerResourceDocument::new)
                .thenApply(this::getCertificatesRequest)
                .thenCompose(this::sendCertificateRequest)
                .whenComplete((rd, throwable) ->
                {
                    if (throwable != null) {
                        logger.error("Could not request certificate", throwable);
                    }
                }).toCompletableFuture();
    }

    private Optional<Link> getCertificatesRequest(ServerResourceDocument certificatesHostApi) {
        return certificatesHostApi.getServiceByType("certificates").flatMap(sro ->
                sro.getLinkByRel("request"));
    }

    private CompletionStage<Void> sendCertificateRequest(Optional<Link> requestLink) {
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
                        .thenAccept(insertRequestedCertificate(issuedCertKeyPair));
            } catch (Throwable ex) {
                return CompletableFuture.<Void>failedStage(ex);
            }
        }).orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("No request link in certificates API")));
    }

    private Consumer<ServiceResourceObject> insertRequestedCertificate(KeyPair issuedCertKeyPair) {
        return sro ->
        {
            sro.getAttributes().attributes().getString("pem").ifPresent(certificatePem ->
            {
                try {
                    // Update keystore
                    List<X509Certificate> chain = new ArrayList<>();
                    PEMParser pemParser = new PEMParser(new StringReader(certificatePem));
                    X509CertificateHolder certificate;
                    while ((certificate = (X509CertificateHolder)pemParser.readObject()) != null)
                    {
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

                } catch (Throwable e) {
                    throw new CompletionException("Could not update keystore with new certificate", e);
                }
            });
        };
    }

    public CompletableFuture<Void> renewCertificate(String certificatesHost) {
        return client.get(new Link("self", certificatesHost))
                .thenApply(ServerResourceDocument::new)
                .thenApply(this::getCertificatesRenewal)
                .thenCompose(this::sendCertificateRenewal)
                .whenComplete((rd, throwable) ->
                {
                    if (throwable != null) {
                        logger.error("Could not request certificate", throwable);
                    }
                }).toCompletableFuture();
    }

    private Optional<Link> getCertificatesRenewal(ServerResourceDocument certificatesHostApi) {
        return certificatesHostApi.getServiceByType("certificates").flatMap(sro ->
                sro.getLinkByRel("renewal"));
    }

    private CompletionStage<Void> sendCertificateRenewal(Optional<Link> requestLink) {
        return requestLink.map(link ->
        {
            try {
                return client.get(link)
                        .thenAccept(this::insertRenewedCertificate);
            } catch (Throwable ex) {
                return CompletableFuture.<Void>failedStage(ex);
            }
        }).orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("No request link in certificates API")));
    }

    private void insertRenewedCertificate(ResourceDocument rd) {
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
                        while ((certificate = (X509CertificateHolder)pemParser.readObject()) != null)
                        {
                            chain.add(new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificate));
                        }

                        char[] password = configuration.getString("keystores.keystore.password").map(String::toCharArray).orElse(null);
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
                        keyStore.setKeyEntry(alias, privateKey, password,chain.toArray(new java.security.cert.Certificate[0]) );

                        // Save it
                        keyStores.save(keyStore);

                        logger.info("Successfully renewed service certificate");
                    } catch (Throwable e) {
                        throw new CompletionException("Could not update keystore with renewed certificate", e);
                    }
                });
            });
        });
    }

    private String createRequest(KeyPair issuedCertKeyPair) throws IOException, GeneralSecurityException, OperatorCreationException {
        X500Name issuedCertSubject = new X500Name("CN=server");

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic());

        // Sign the new KeyPair with the Provisioning cert Private Key
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC");
        PrivateKey provisioning = (PrivateKey) keyStore.getKey("provisioning", "password".toCharArray());
        if (provisioning == null)
        {
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
}
