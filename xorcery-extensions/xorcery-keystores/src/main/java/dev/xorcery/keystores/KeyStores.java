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
package dev.xorcery.keystores;

import com.fasterxml.jackson.databind.node.MissingNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.secrets.Secrets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.xorcery.lang.Exceptions.unwrap;

public class KeyStores {

    private final KeyStoresConfiguration configuration;
    private final Secrets secrets;
    private final Logger logger;
    private final Map<String, KeyStore> keyStores = new ConcurrentHashMap<>();
    private final KeyPairGenerator keyGen;

    public KeyStores(Configuration configuration, Secrets secrets, Logger logger) throws NoSuchAlgorithmException, NoSuchProviderException {
        this.configuration = KeyStoresConfiguration.get(configuration);
        this.secrets = secrets;
        this.logger = logger;

        Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        if (null == Security.getProvider(p.getName())) {
            Security.addProvider(p);
        }

        keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(256, new SecureRandom());
    }

    /**
     * Called when keyStore is changed through save. Subclasses may override this method in order to act when
     * a keystore is changed.
     *
     * @param keyStore the key-store that has changed.
     */
    protected void publish(KeyStore keyStore) {
    }

    public KeyStore getKeyStore(String configurationPrefix) {
        return keyStores.computeIfAbsent(configurationPrefix, this::loadKeyStore
        );
    }

    public void save(KeyStore keyStore) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        for (Map.Entry<String, KeyStore> stringKeyStoreEntry : keyStores.entrySet()) {
            if (stringKeyStoreEntry.getValue() == keyStore) {
                String name = stringKeyStoreEntry.getKey();
                KeyStoreConfiguration keyStoreConfiguration = configuration.getKeyStoreConfiguration(name).orElseThrow();
                File keyStoreFile = keyStoreConfiguration.getPath();
                File parentFile = keyStoreFile.getParentFile();
                if (!parentFile.exists()) {
                    logger.info("Creating keystore directory:" + parentFile);
                    if (!parentFile.mkdirs()) {
                        logger.warn("Could not create keystore directory");
                    }
                }
                try (FileOutputStream outputStream = new FileOutputStream(keyStoreFile)) {
                    keyStore.store(outputStream, keyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElse(null));
                }
                logger.info("Saved keystore " + name + " to " + keyStoreFile);
                publish(keyStore);
                return;
            }
        }
        throw new IllegalArgumentException("Not a managed KeyStore");
    }

    protected void addDefaultRootCaCertificates(KeyStore trustStore) throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // Loads default Root CA certificates (generally, from JAVA_HOME/lib/cacerts)
        trustManagerFactory.init((KeyStore) null);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
                    trustStore.setCertificateEntry(acceptedIssuer.getSubjectX500Principal().getName(), acceptedIssuer);
                }
            }
        }
    }

    private KeyStore loadKeyStore(String keyStoreName) {
        KeyStoreConfiguration keyStoreConfiguration = configuration.getKeyStoreConfiguration(keyStoreName).orElseThrow(Configuration.missing(keyStoreName));
        File keyStoreFile = keyStoreConfiguration.getPath();
        try {
            KeyStore keyStore = KeyStore.getInstance(keyStoreConfiguration.getType());

            if (!keyStoreFile.exists()) {
                if (!keyStoreConfiguration.configuration().getJson("template").orElse(MissingNode.getInstance()).isMissingNode()) {
                    // Copy template to file
                    URL templateStoreUrl = keyStoreConfiguration.configuration().getResourceURL("template").orElseThrow(() -> new IllegalArgumentException("Template file does not exist for keystore " + keyStoreName));
                    char[] password = keyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElse(null);
                    // Copy template to file
                    try (InputStream inputStream = templateStoreUrl.openStream()) {
                        LogManager.getLogger(getClass()).info("Copying template keystore " + templateStoreUrl + " to local keystore file " + keyStoreFile);
                        File keyStoreDir = keyStoreFile.getParentFile();
                        if (!keyStoreDir.exists()) {
                            keyStoreDir.mkdirs();
                        }
                        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
                            fileOutputStream.write(inputStream.readAllBytes());
                        }
                    } catch (FileNotFoundException e) {
                        // Continue with empty keystore
                        LogManager.getLogger(getClass()).warn("Could not find template keystore, continuing with empty keystore " + keyStoreName, e);
                        // Create empty store
                        keyStore.load(null, password);
                        File keyStoreDir = keyStoreFile.getParentFile();
                        if (!keyStoreDir.exists()) {
                            keyStoreDir.mkdirs();
                        }
                        try (FileOutputStream outputStream = new FileOutputStream(keyStoreFile)) {
                            keyStore.store(outputStream, password);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not copy template store:" + templateStoreUrl, e);
                    }
                } else {
                    // Create empty store
                    char[] password = keyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElse(null);
                    keyStore.load(null, password);
                    File keyStoreDir = keyStoreFile.getParentFile();
                    if (!keyStoreDir.exists()) {
                        keyStoreDir.mkdirs();
                    }
                    try (FileOutputStream outputStream = new FileOutputStream(keyStoreFile)) {
                        keyStore.store(outputStream, password);
                    }
                    LogManager.getLogger(getClass()).info("Created empty keystore " + keyStoreName);
                }
            }

            try (InputStream inStream = new FileInputStream(keyStoreFile)) {
                keyStore.load(inStream, keyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElse(null));

                if (keyStoreConfiguration.isAddRootCa()) {
                    addDefaultRootCaCertificates(keyStore);
                }

                LogManager.getLogger(getClass()).info("Loaded keystore " + keyStoreName + "(" + keyStoreFile + ")");
                {
                    StringBuilder builder = new StringBuilder();
                    keyStore.aliases().asIterator().forEachRemaining(alias -> builder.append("\n").append(alias));
                    LogManager.getLogger(getClass()).debug(keyStoreName + " aliases:" + builder);
                }

                return keyStore;
            } catch (Exception ex) {
                URL templateStoreUrl = keyStoreConfiguration.getTemplate();
                if (templateStoreUrl != null) {
                    // Copy template to file
                    File keyStoreDir = keyStoreFile.getParentFile();
                    if (!keyStoreDir.exists()) {
                        keyStoreDir.mkdirs();
                    }

                    try (InputStream inputStream = templateStoreUrl.openStream()) {
                        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
                            fileOutputStream.write(inputStream.readAllBytes());
                        }
                    }
                    // Try again
                    return loadKeyStore(keyStoreName);
                } else {
                    throw ex;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Could not load keystore '" + keyStoreName + "' from " + keyStoreFile, unwrap(e));
        }
    }

    // Utility methods
    public KeyPair getOrCreateKeyPair(String alias, String keyStoreName) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException {
        KeyStoreConfiguration keyStoreConfiguration = configuration.getKeyStoreConfiguration(keyStoreName).orElseThrow(Configuration.missing(keyStoreName));
        KeyStore keyStore = getKeyStore(keyStoreName);
        char[] password = keyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElse(null);

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
        if (privateKey == null) {
            KeyPair keyPair = keyGen.generateKeyPair();
            X509Certificate certificate = selfSign(keyPair, "cn=" + alias);
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, new Certificate[]{certificate});
            save(keyStore);
            return keyPair;
        } else {
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
            return new KeyPair(publicKey, privateKey);
        }
    }

    public X509Certificate selfSign(KeyPair keyPair, String subjectDN) throws OperatorCreationException, CertificateException, IOException {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);

        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        X500Name dnName = new X500Name(subjectDN);
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        // Basically never expire
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1000);

        Date endDate = calendar.getTime();

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withECDSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic());
        BasicConstraints basicConstraints = new BasicConstraints(true);
        certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints);
        return new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(contentSigner));
    }
}
