package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.certificates.KeyStores;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

@Service
public class IntermediateCA {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Configuration configuration;
    private final KeyStore intermediateCaKeyStore;
    private final KeyStore intermediateCaTrustStore;

    private final JcaX509ExtensionUtils issuedCertExtUtils;
    private final String keyStoreAlias;

    @Inject
    public IntermediateCA(ServiceResourceObjects serviceResourceObjects,
                          Configuration configuration,
                          KeyStores keyStores
    ) throws NoSuchAlgorithmException {
        this.configuration = configuration;

        intermediateCaKeyStore = keyStores.getKeyStore("certificates.keystore");
        intermediateCaTrustStore = keyStores.getKeyStore("certificates.truststore");

        issuedCertExtUtils = new JcaX509ExtensionUtils();

        keyStoreAlias = configuration.getString("certificates.server.alias").orElse("intermediate");

        serviceResourceObjects.add(new ServiceResourceObject.Builder(() -> configuration, "certificates")
                .with(b ->
                {
                    b.api("request", "api/certificates/request");
                    b.api("renewal", "api/certificates/renewal");
                    b.api("certificate", "api/certificates/rootca.cer");
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

    public String createCertificate(X500Name subject, SubjectPublicKeyInfo publicKeyInfo, List<String> ipAddresses) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException, PKCSException {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, configuration.getInteger("certificates.server.validity").orElse(90));
        X509Certificate serviceCaCert = (X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias);
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        X500Name issuerName = X500Name.getInstance(serviceCaCert.getSubjectX500Principal().getEncoded());
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(issuerName, issuedCertSerialNum, new Date(), now.getTime(), subject, publicKeyInfo);

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
        char[] password = configuration.getString("certificates.truststore.password").map(String::toCharArray).orElse(null);
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

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, configuration.getInteger("certificates.server.validity").orElse(90));
        X509Certificate signingCert = (X509Certificate) intermediateCaKeyStore.getCertificate(keyStoreAlias);
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        X500Name signerName = X500Name.getInstance(signingCert.getIssuerX500Principal().getEncoded());
        X500Name subjectName = X500Name.getInstance(currentClientCertificate.getIssuerX500Principal().getEncoded());
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(signerName, issuedCertSerialNum, new Date(), now.getTime(), subjectName, currentBcClientCertificate.getSubjectPublicKeyInfo());

        // Add Extensions
        Extensions exts = new X509CertificateHolder(currentClientCertificate.getEncoded()).getExtensions();

        // TODO Update DNS names
        for (Enumeration en = exts.oids(); en.hasMoreElements(); ) {
            issuedCertBuilder.addExtension(exts.getExtension((ASN1ObjectIdentifier) en.nextElement()));
        }

        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(PROVIDER_NAME);
        ContentSigner csrContentSigner = csrBuilder.build((PrivateKey) intermediateCaKeyStore.getKey(keyStoreAlias, null));
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

    private String toPEM(X509CertificateHolder certificateHolder) throws CertificateException, IOException {
        X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificateHolder);

        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE, issuedCert.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }
}
