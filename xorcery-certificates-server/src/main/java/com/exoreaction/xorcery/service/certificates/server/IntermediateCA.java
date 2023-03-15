package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import com.exoreaction.xorcery.service.keystores.KeyStoresConfiguration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.*;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

@Service(name = "certificates.server")
public class IntermediateCA {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Configuration configuration;
    private final KeyStore intermediateCaKeyStore;
    private final KeyStore intermediateCaTrustStore;
    private final CertificatesServerConfiguration certificatesServerConfiguration;
    private final KeyStoresConfiguration keystoresConfiguration;
    private KeyStores keyStores;

    private final JcaX509ExtensionUtils issuedCertExtUtils;
    private final String keyStoreAlias;

    @Inject
    public IntermediateCA(ServiceResourceObjects serviceResourceObjects,
                          Configuration configuration,
                          KeyStores keyStores
    ) throws NoSuchAlgorithmException {
        this.configuration = configuration;
        this.certificatesServerConfiguration = new CertificatesServerConfiguration(configuration.getConfiguration("certificates.server"));
        this.keystoresConfiguration = new KeyStoresConfiguration(configuration.getConfiguration("keystores"));

        intermediateCaKeyStore = keyStores.getKeyStore("keystore");
        intermediateCaTrustStore = keyStores.getKeyStore("truststore");
        this.keyStores = keyStores;

        issuedCertExtUtils = new JcaX509ExtensionUtils();

        keyStoreAlias = certificatesServerConfiguration.getAlias();

        serviceResourceObjects.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "certificates")
                .with(b ->
                {
                    b.api("request", "api/certificates/request");
                    b.api("renewal", "api/certificates/renewal");
                    b.api("certificate", "api/certificates/rootca.cer");
                    b.api("crl", "api/certificates/intermediateca.crl");
                })
                .build());
    }

    public String requestCertificate(String csrPemEncoded, List<String> ipAddresses) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException, PKCSException {

        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) new PEMParser(new StringReader(csrPemEncoded)).readObject();

        X509Certificate rootCert = (X509Certificate) intermediateCaTrustStore.getCertificate("provisioning");
        boolean isValid = csr.isSignatureValid(new JcaContentVerifierProviderBuilder().build(rootCert));
        if (!isValid)
            throw new IllegalArgumentException("CSR not signed by provisioning CA");

        return createCertificate(csr.getSubject(), csr.getSubjectPublicKeyInfo(), ipAddresses);
    }

    public String createCertificate(X500Name subject, SubjectPublicKeyInfo publicKeyInfo, List<String> ipAddresses) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime validFrom = now.minusDays(1); // backdated to protect against clock skew and misconfigurations
        ZonedDateTime expiryDate = now.plus(certificatesServerConfiguration.getValidity());
        X509Certificate serviceCaCert = (X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias);
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        X500Name issuerName = X500Name.getInstance(serviceCaCert.getSubjectX500Principal().getEncoded());
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(issuerName, issuedCertSerialNum, Date.from(validFrom.toInstant()), Date.from(expiryDate.toInstant()), subject, publicKeyInfo);

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                issuedCertExtUtils.createAuthorityKeyIdentifier(serviceCaCert.getPublicKey(),
                        GeneralNames.getInstance(new DERSequence(new GeneralName(X500Name.getInstance(serviceCaCert.getSubjectX500Principal().getEncoded())))), serviceCaCert.getSerialNumber()));
        issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(publicKeyInfo));

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature));

        // Add localhost, IP, and DNS names to be used for SSL
        List<GeneralName> names = new ArrayList<>();
        names.add(new GeneralName(GeneralName.dNSName, "localhost"));
        for (String ipAddress : ipAddresses) {
            names.add(new GeneralName(GeneralName.iPAddress, ipAddress));
        }
        // TODO Lookup DNS names here, or have them as parameter to this method
        issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(names.toArray(new ASN1Encodable[1])));

        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(PROVIDER_NAME);
        char[] password = keystoresConfiguration.getKeyStoreConfiguration("keystore").getPassword();
        ContentSigner csrContentSigner = csrBuilder.build((PrivateKey) intermediateCaKeyStore.getKey(keyStoreAlias, password));
        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);

        // Create trust chain
        String pem = toPEM(issuedCertHolder);
        pem += toPEM(new JcaX509CertificateHolder((X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias)));
        pem += toPEM(new JcaX509CertificateHolder((X509Certificate) intermediateCaTrustStore.getCertificate("root")));
        return pem;
    }

    public String renewCertificate(X509Certificate currentClientCertificate) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException {

        X509CertificateHolder currentBcClientCertificate = new X509CertificateHolder(currentClientCertificate.getEncoded());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plus(certificatesServerConfiguration.getValidity());
        X509Certificate signingCert = (X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias);
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        X500Name signerName = X500Name.getInstance(signingCert.getIssuerX500Principal().getEncoded());
        X500Name subjectName = X500Name.getInstance(currentClientCertificate.getIssuerX500Principal().getEncoded());
        LocalDateTime oneDayBeforeNow = now.minusDays(1); // backdated to protect against clock skew and misconfigurations
        Date notBefore = Date.from(oneDayBeforeNow.toInstant(ZoneOffset.systemDefault().getRules().getOffset(oneDayBeforeNow)));
        Date notAfter = Date.from(expiryDate.toInstant(ZoneOffset.systemDefault().getRules().getOffset(expiryDate)));
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(signerName, issuedCertSerialNum, notBefore, notAfter, subjectName, currentBcClientCertificate.getSubjectPublicKeyInfo());

        // Add Extensions
        Extensions exts = new X509CertificateHolder(currentClientCertificate.getEncoded()).getExtensions();

        // TODO Update DNS names
        for (Enumeration en = exts.oids(); en.hasMoreElements(); ) {
            issuedCertBuilder.addExtension(exts.getExtension((ASN1ObjectIdentifier) en.nextElement()));
        }

        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(PROVIDER_NAME);
        char[] password = keystoresConfiguration.getKeyStoreConfiguration("keystore").getPassword();
        ContentSigner csrContentSigner = csrBuilder.build((PrivateKey) intermediateCaKeyStore.getKey(keyStoreAlias, password));
        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);

        // Create trust chain
        String pem = toPEM(issuedCertHolder);
        pem += toPEM(new JcaX509CertificateHolder((X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias)));
        pem += toPEM(new JcaX509CertificateHolder((X509Certificate) intermediateCaTrustStore.getCertificate("root")));
        return pem;
    }

    public byte[] getCertificate() throws KeyStoreException, CertificateEncodingException {
        return intermediateCaKeyStore.getCertificate("root").getEncoded();
    }

    public String getCRL() throws GeneralSecurityException, IOException, OperatorCreationException {
        StringWriter stringWriter = new StringWriter();

        {
            X509Certificate signingCert = (X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias);
            char[] password = keystoresConfiguration.getKeyStoreConfiguration("keystore").getPassword();
            X509CRL crl = createEmptyCRL((PrivateKey) intermediateCaKeyStore.getKey(keyStoreAlias, password), signingCert);

            PemWriter pWrt = new PemWriter(stringWriter);
            pWrt.writeObject(new PemObject(PEMParser.TYPE_X509_CRL, crl.getEncoded()));
            pWrt.close();
        }

        // See if we have the rootstore as well, and append it
        try {
            KeyStore rootStore = keyStores.getKeyStore("rootstore");

            X509Certificate signingCert = (X509Certificate) rootStore.getCertificate("root");
            char[] password = keystoresConfiguration.getKeyStoreConfiguration("rootstore").getPassword();
            X509CRL crl = createEmptyCRL((PrivateKey) rootStore.getKey("root", password), signingCert);
            PemWriter pWrt = new PemWriter(stringWriter);
            pWrt.writeObject(new PemObject(PEMParser.TYPE_X509_CRL, crl.getEncoded()));
            pWrt.close();
        } catch (RuntimeException exception) {
            // Ignore
        }

        return stringWriter.toString();
    }

    private String toPEM(X509CertificateHolder certificateHolder) throws CertificateException, IOException {
        X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificateHolder);

        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE, issuedCert.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }

    private X509CRL createEmptyCRL(
            PrivateKey caKey,
            X509Certificate caCert)
            throws IOException, GeneralSecurityException, OperatorCreationException {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        Date nowDate = new Date(now.toInstant(ZoneOffset.UTC).toEpochMilli());
        X509v2CRLBuilder crlGen = new JcaX509v2CRLBuilder(caCert.getSubjectX500Principal(), nowDate);

        Date nextMilleniumDate = new Date(now.plus(1000, ChronoField.YEAR.getBaseUnit()).toInstant(ZoneOffset.UTC).toEpochMilli());
        crlGen.setNextUpdate(nextMilleniumDate);

        // add extensions to CRL
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        crlGen.addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(caCert));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                .setProvider("BC").build(caKey);

        JcaX509CRLConverter converter = new JcaX509CRLConverter().setProvider("BC");

        return converter.getCRL(crlGen.build(signer));
    }

}
