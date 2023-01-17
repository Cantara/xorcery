package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.StringReader;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

@Service(name = "certificates.server.self")
@RunLevel(value = 2)
public class SelfCertificateService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final String certificateAlias;

    private final KeyStore keyStore;
    private final KeyStores keyStores;
    private final Configuration configuration;
    private final IntermediateCA intermediateCA;

    @Inject
    public SelfCertificateService(KeyStores keyStores,
                                  Configuration configuration,
                                  IntermediateCA intermediateCA) throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException {
        this.keyStore = keyStores.getKeyStore("keystores.keystore");
        this.keyStores = keyStores;
        this.configuration = configuration;
        this.intermediateCA = intermediateCA;
        certificateAlias = configuration.getString("server.ssl.alias").orElse("self");

        if (keyStore.containsAlias(certificateAlias)) {
            // Renewal?
            renewCertificate();
        } else {
            // Request
            requestCertificate();
        }
    }

    private void renewCertificate() {
    }

    private void requestCertificate() throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", PROVIDER_NAME);
        keyGen.initialize(256, new SecureRandom());
        KeyPair issuedCertKeyPair = keyGen.generateKeyPair();

        List<String> inetAddresses = new ArrayList<>();
        NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(ni ->
                ni.getInetAddresses().asIterator().forEachRemaining(ia ->
                {
                    if (ia instanceof Inet4Address i4a)
                        inetAddresses.add(ia.getHostAddress());
                }));
        // TODO Add DNS names here
        System.out.println(inetAddresses);
        StandardConfiguration standardConfiguration = () -> configuration;
        X500Name issuedCertSubject = new X500Name("CN=" + standardConfiguration.getId());
        String certificatePem = intermediateCA.createCertificate(issuedCertSubject, SubjectPublicKeyInfo.getInstance(issuedCertKeyPair.getPublic().getEncoded()), inetAddresses);
        insertSelfCertificate(certificatePem, issuedCertKeyPair);
    }

    private void insertSelfCertificate(String certificatePem, KeyPair issuedCertKeyPair) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        List<X509Certificate> chain = new ArrayList<>();
        PEMParser pemParser = new PEMParser(new StringReader(certificatePem));
        X509CertificateHolder certificate;
        while ((certificate = (X509CertificateHolder)pemParser.readObject()) != null)
        {
            chain.add(new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificate));
        }
        char[] password = configuration.getString("keystores.keystore.password").map(String::toCharArray).orElse(null);
        keyStore.setKeyEntry(certificateAlias, issuedCertKeyPair.getPrivate(), password, chain.toArray(new java.security.cert.Certificate[0]));
        keyStores.save(keyStore);

        logger.info("Updated certificate for SSL client/server authentication");
    }
}
