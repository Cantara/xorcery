package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyStores {
    private final KeyStoresConfiguration configuration;
    private final Map<String, KeyStore> keyStores = new ConcurrentHashMap<>();

    public KeyStores(Configuration configuration) {
        this.configuration = new KeyStoresConfiguration(configuration.getConfiguration("keystores"));

        Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        if (null == Security.getProvider(p.getName())) {
            Security.addProvider(p);
        }
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

    public KeyStore getDefaultKeyStore() {
        return getKeyStore("keystore");
    }

    public KeyStore getDefaultTrustStore() {
        return getKeyStore("truststore");
    }

    public void save(KeyStore keyStore) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        for (Map.Entry<String, KeyStore> stringKeyStoreEntry : keyStores.entrySet()) {
            if (stringKeyStoreEntry.getValue() == keyStore) {
                String name = stringKeyStoreEntry.getKey();
                KeyStoreConfiguration keyStoreConfiguration = configuration.getKeyStoreConfiguration(name);
                URL keyStoreUrl = keyStoreConfiguration.getURL();
                try (FileOutputStream outputStream = new FileOutputStream(keyStoreUrl.getFile())) {
                    keyStore.store(outputStream, keyStoreConfiguration.getPassword());
                }
                LogManager.getLogger(getClass()).info("Saved keystore " + name);
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
        URL keyStoreUrl = null;
        try {
            KeyStoreConfiguration keyStoreConfiguration = configuration.getKeyStoreConfiguration(keyStoreName);
            KeyStore keyStore = KeyStore.getInstance(keyStoreConfiguration.getType());

            if (keyStoreConfiguration.configuration().has("template")) {
                File keyStoreOutput = new File(".", keyStoreConfiguration.getPath()).getAbsoluteFile();
                if (!keyStoreOutput.exists()) {
                    // Copy template to file
                    URL templateStoreUrl = keyStoreConfiguration.configuration().getResourceURL("template").orElseThrow(() -> new IllegalArgumentException("Template file does not exist for keystore " + keyStoreName));
                    // Copy template to file
                    try (InputStream inputStream = templateStoreUrl.openStream()) {
                        LogManager.getLogger(getClass()).info("Copying template keystore " + templateStoreUrl + " to local keystore file " + keyStoreOutput);
                        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreOutput)) {
                            fileOutputStream.write(inputStream.readAllBytes());
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                keyStoreUrl = keyStoreOutput.toURI().toURL();
            } else {
                keyStoreUrl = keyStoreConfiguration.getURL();
            }

            try (InputStream inStream = keyStoreUrl.openStream()) {
                keyStore.load(inStream, keyStoreConfiguration.getPassword());

                if (keyStoreConfiguration.isAddRootCa()) {
                    addDefaultRootCaCertificates(keyStore);
                }

                LogManager.getLogger(getClass()).info("Loaded keystore " + keyStoreName + "(" + keyStoreUrl + ")");
                {
                    StringBuilder builder = new StringBuilder();
                    keyStore.aliases().asIterator().forEachRemaining(alias -> builder.append(alias).append("\n"));
                    LogManager.getLogger(getClass()).debug(keyStoreName + " aliases:\n" + builder);
                }

                return keyStore;
            } catch (Exception ex) {
                String template = keyStoreConfiguration.getTemplate();
                if (template != null && keyStoreUrl.getProtocol().equals("file")) {
                    // Copy template to file
                    URL templateStoreUrl = new URL(template);
                    try (InputStream inputStream = templateStoreUrl.openStream()) {
                        try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreUrl.getFile())) {
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
            throw new RuntimeException("Could not load keystore " + keyStoreName + " from " + keyStoreUrl, e);
        }
    }
}
