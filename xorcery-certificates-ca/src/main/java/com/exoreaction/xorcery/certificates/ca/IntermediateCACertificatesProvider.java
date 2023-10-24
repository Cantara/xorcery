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
package com.exoreaction.xorcery.certificates.ca;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.keystores.KeyStoresConfiguration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;


@Service(name = "intermediateca")
@ContractsProvided({CertificatesProvider.class, IntermediateCACertificatesProvider.class})
public class IntermediateCACertificatesProvider
        implements CertificatesProvider {

    private static final String KEYSTORE_NAME = "castore";

    private final Logger logger = LogManager.getLogger(getClass());

    private final IntermediateCaConfiguration intermediateCaConfiguration;
    private final KeyStore intermediateCaKeyStore;
    private final KeyStore trustStore;
    private final JcaX509ExtensionUtils issuedCertExtUtils;
    private final String caCertAlias;
    private final JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider(PROVIDER_NAME);
    private final KeyStoresConfiguration keyStoresConfiguration;
    private final KeyStores keyStores;
    private final Secrets secrets;

    @Inject
    public IntermediateCACertificatesProvider(Configuration configuration, KeyStores keyStores, Secrets secrets) throws NoSuchAlgorithmException {
        intermediateCaConfiguration = new IntermediateCaConfiguration(configuration.getConfiguration("intermediateca"));
        intermediateCaKeyStore = keyStores.getKeyStore(KEYSTORE_NAME);
        trustStore = keyStores.getKeyStore("truststore");
        keyStoresConfiguration = new KeyStoresConfiguration(configuration.getConfiguration("keystores"));
        this.keyStores = keyStores;
        this.secrets = secrets;

        issuedCertExtUtils = new JcaX509ExtensionUtils();
        caCertAlias = intermediateCaConfiguration.getAlias();
    }

    @Override
    public CompletionStage<List<X509Certificate>> requestCertificates(PKCS10CertificationRequest csr) {
        try {
            X500Name issuedCertSubject = csr.getSubject();
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime validFrom = now.minusDays(1); // backdated to protect against clock skew and misconfigurations
            ZonedDateTime expiryDate = now.plus(intermediateCaConfiguration.getValidity());
            X509Certificate serviceCaCert = (X509Certificate) intermediateCaKeyStore.getCertificate(caCertAlias);
            BigInteger issuedCertSerialNum = new BigInteger(Long.toString(System.currentTimeMillis()));
            X500Name issuerName = X500Name.getInstance(serviceCaCert.getSubjectX500Principal().getEncoded());
            X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(issuerName, issuedCertSerialNum, Date.from(validFrom.toInstant()), Date.from(expiryDate.toInstant()), issuedCertSubject, csr.getSubjectPublicKeyInfo());

            // Add Extensions
            // Use BasicConstraints to say that this Cert is not a CA
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

            // Add Issuer cert identifier as Extension
            issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                    issuedCertExtUtils.createAuthorityKeyIdentifier(serviceCaCert.getPublicKey(),
                            GeneralNames.getInstance(new DERSequence(new GeneralName(issuerName))), serviceCaCert.getSerialNumber()));
            issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(serviceCaCert.getPublicKey()));

            for (ASN1ObjectIdentifier extensionOID : csr.getRequestedExtensions().getExtensionOIDs()) {
                logger.debug("Copying {} from CSR into cert", extensionOID.toString());
                if (extensionOID != Extension.subjectAlternativeName) {
                    issuedCertBuilder.addExtension(csr.getRequestedExtensions().getExtension(extensionOID));
                }
            }

            // Add requested DNS names and IP addresses
            List<GeneralName> names = new ArrayList<>();
            Extension san = csr.getRequestedExtensions().getExtension(Extension.subjectAlternativeName);
            if (san != null) {
                GeneralNames generalNames = GeneralNames.getInstance(san.getExtnValue().getOctets());

                for (GeneralName name : generalNames.getNames()) {
                    if (name.getTagNo() == GeneralName.dNSName) {
                        names.add(name);
                    } else if (name.getTagNo() == GeneralName.iPAddress) {
                        names.add(name);
                    }
                }
            }

            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(names.toArray(new ASN1Encodable[0])));

            JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(PROVIDER_NAME);
            char[] password = keyStoresConfiguration.getKeyStoreConfiguration("ssl").getPassword()
                    .map(secrets::getSecretString).map(String::toCharArray).orElse(null);
            ContentSigner csrContentSigner = csrBuilder.build((PrivateKey) intermediateCaKeyStore.getKey(caCertAlias, password));
            X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);

            // Create trust chain
            List<X509Certificate> certChain = new ArrayList<>();
            certChain.add(certificateConverter.getCertificate(issuedCertHolder));
            certChain.add((X509Certificate) intermediateCaKeyStore.getCertificate(caCertAlias));
            certChain.add((X509Certificate) trustStore.getCertificate("root"));

            return CompletableFuture.completedStage(certChain);
        } catch (Throwable throwable) {
            return CompletableFuture.failedStage(throwable);
        }
    }

    public byte[] getRootCertificate() throws KeyStoreException, CertificateEncodingException {
        return intermediateCaKeyStore.getCertificate("root").getEncoded();
    }

    public String getCRL() throws GeneralSecurityException, IOException, OperatorCreationException {
        StringWriter stringWriter = new StringWriter();

        {
            X509Certificate signingCert = (X509Certificate) intermediateCaKeyStore.getCertificate(caCertAlias);
            char[] password = keyStoresConfiguration.getKeyStoreConfiguration(KEYSTORE_NAME).getPassword()
                    .map(secrets::getSecretString).map(String::toCharArray).orElse(null);
            X509CRL crl = createEmptyCRL((PrivateKey) intermediateCaKeyStore.getKey(caCertAlias, password), signingCert);

            PemWriter pWrt = new PemWriter(stringWriter);
            pWrt.writeObject(new PemObject(PEMParser.TYPE_X509_CRL, crl.getEncoded()));
            pWrt.close();
        }

        // See if we have the rootstore as well, and append it
        try {
            KeyStore rootStore = keyStores.getKeyStore("rootstore");

            X509Certificate signingCert = (X509Certificate) rootStore.getCertificate("root");
            char[] password = keyStoresConfiguration.getKeyStoreConfiguration("rootstore").getPassword()
                    .map(secrets::getSecretString).map(String::toCharArray).orElse(null);
            X509CRL crl = createEmptyCRL((PrivateKey) rootStore.getKey("root", password), signingCert);
            PemWriter pWrt = new PemWriter(stringWriter);
            pWrt.writeObject(new PemObject(PEMParser.TYPE_X509_CRL, crl.getEncoded()));
            pWrt.close();
        } catch (RuntimeException exception) {
            // Ignore
        }

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
