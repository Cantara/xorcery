package com.exoreaction.xorcery.service.certificates.client;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
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
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

@Disabled
public class CertificateRequestTest {

    private KeyStore serviceCaKeyStore;
    private KeyStore keyStore;
    private KeyStore trustStore;

    @Test
    public void testCertificateRequest() throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException {
        Security.addProvider(new BouncyCastleProvider());

        serviceCaKeyStore = KeyStore.getInstance(new File("../xorcery-certificates-server/src/main/resources/META-INF/intermediatecakeystore.p12").getCanonicalFile(), "password".toCharArray());
        keyStore = KeyStore.getInstance(new File("target/classes/META-INF/keystore.p12"), "password".toCharArray());
        trustStore = KeyStore.getInstance(new File("target/classes/META-INF/truststore.p12"), "password".toCharArray());

        System.out.println("Manager truststore");
        serviceCaKeyStore.aliases().asIterator().forEachRemaining(System.out::println);
        System.out.println("Client keystore");
        keyStore.aliases().asIterator().forEachRemaining(System.out::println);
        System.out.println("Client truststore");
        trustStore.aliases().asIterator().forEachRemaining(System.out::println);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(256, new SecureRandom());
        KeyPair issuedCertKeyPair = keyGen.generateKeyPair();

        String csr = createRequest(issuedCertKeyPair);
        System.out.println(csr);

        String certificate = createCertificate(csr);
        System.out.println(certificate);

        verifyCertificate(certificate);

        storeCertificate(issuedCertKeyPair, certificate);
    }

    private String createRequest(KeyPair issuedCertKeyPair) throws IOException, GeneralSecurityException, OperatorCreationException {
        X500Name issuedCertSubject = new X500Name("CN=server");

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic());

        // Sign the new KeyPair with the Provisioning cert Private Key
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC");
        ContentSigner csrContentSigner = csrBuilder.build((PrivateKey) keyStore.getKey("provisioning", "password".toCharArray()));
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE_REQUEST, csr.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }

    private String createCertificate(String csrEncoded) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException, PKCSException {

        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) new PEMParser(new StringReader(csrEncoded)).readObject();

        X509Certificate provisioningCert = (X509Certificate) keyStore.getCertificate("provisioning");
        boolean isValid = csr.isSignatureValid(new JcaContentVerifierProviderBuilder().build(provisioningCert));
        if (!isValid)
            throw new IllegalArgumentException("CSR not signed by provisioning CA");

        Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, 9000);
        X509Certificate signingCert = (X509Certificate) serviceCaKeyStore.getCertificate("intermediate");
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        X500Name signerName = X500Name.getInstance(signingCert.getIssuerX500Principal().getEncoded());
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(signerName, issuedCertSerialNum, new Date(), now.getTime(), csr.getSubject(), csr.getSubjectPublicKeyInfo());

        JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils();

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(signingCert));
        issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature));

        // Add DNS name is cert is to used for SSL
        issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[]{
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1")
        }));

        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC");
        ContentSigner csrContentSigner = csrBuilder.build((PrivateKey) serviceCaKeyStore.getKey("intermediate", "password".toCharArray()));
        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
        X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(issuedCertHolder);

        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE, issuedCert.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }

    private void verifyCertificate(String certificate) throws IOException, NoSuchAlgorithmException, KeyStoreException {
/*
        X509CertificateHolder certificateHolder = (X509CertificateHolder) new PEMParser(new StringReader(certificate)).readObject();

        X509Certificate provisioningCert = (X509Certificate) keyStore.getCertificate("provisioning");
        provisioningCert.
                trustStore.getCertificate().
        boolean isValid = certificateHolder.isSignatureValid(new JcaContentVerifierProviderBuilder().build(provisioningCert));
        if (!isValid)
            throw new IllegalArgumentException("CSR not signed by provisioning CA");


        certificateHolder.isSignatureValid()

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            System.out.println(trustManager);
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                java.security.cert.Certificate certificate1 = certificateHolder.toASN1Structure();

                x509TrustManager.checkClientTrusted(certificateHolder.toASN1Structure());
            }
        }
*/
    }

    private void storeCertificate(KeyPair issuedCertKeyPair, String certificate) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {

        X509CertificateHolder certificateHolder = (X509CertificateHolder) new PEMParser(new StringReader(certificate)).readObject();
        X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificateHolder);

        keyStore.setKeyEntry("server", issuedCertKeyPair.getPrivate(), null, new Certificate[]{issuedCert});
        keyStore.deleteEntry("provisioning");
        FileOutputStream outputStream = new FileOutputStream(new File("target/classes/META-INF/keystore.p12"));
        keyStore.store(outputStream, "password".toCharArray());
        outputStream.close();
    }


}
