package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
    private final Configuration configuration;
    private final Map<String, KeyStore> keyStores = new ConcurrentHashMap<>();

    public KeyStores(Configuration configuration) {
        this.configuration = configuration;

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
        return getKeyStore("keystores.keystore");
    }

    public KeyStore getDefaultTrustStore() {
        return getKeyStore("keystores.truststore");
    }

    public void save(KeyStore keyStore) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        for (Map.Entry<String, KeyStore> stringKeyStoreEntry : keyStores.entrySet()) {
            if (stringKeyStoreEntry.getValue() == keyStore) {
                String name = stringKeyStoreEntry.getKey();
                Configuration keyStoreConfiguration = configuration.getConfiguration(name);
                URL keyStoreUrl = keyStoreConfiguration.getResourceURL("path").orElseThrow(() -> new IllegalArgumentException("Missing " + name + ".path"));
                try (FileOutputStream outputStream = new FileOutputStream(keyStoreUrl.getFile())) {
                    keyStore.store(outputStream, keyStoreConfiguration.getString("password").map(String::toCharArray).orElse(null));
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
            Configuration keyStoreConfiguration = configuration.getConfiguration(keyStoreName);
            KeyStore keyStore = KeyStore.getInstance(keyStoreConfiguration.getString("type").orElse("PKCS12"));
            keyStoreUrl = keyStoreConfiguration.getResourceURL("path").orElseGet(() ->
            {
                URL templateStoreUrl = keyStoreConfiguration.getResourceURL("template").orElse(null);
                if (templateStoreUrl != null) {
                    try {
                        // Copy template to file
                        try (InputStream inputStream = templateStoreUrl.openStream()) {
                            try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreConfiguration.getString("path").orElseThrow(() -> new IllegalArgumentException("Missing " + keyStoreName + ".path")))) {
                                fileOutputStream.write(inputStream.readAllBytes());
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    // Try again
                    return keyStoreConfiguration.getResourceURL("path")
                            .orElseThrow(() -> new IllegalArgumentException("Missing " + keyStoreName + ".path"));
                } else {
                    throw new IllegalArgumentException("Missing " + keyStoreName + ".path");
                }
            });

            try (InputStream inStream = keyStoreUrl.openStream()) {
                keyStore.load(inStream, keyStoreConfiguration.getString("password").map(String::toCharArray).orElse(null));

                if (keyStoreConfiguration.getBoolean("addrootca").orElse(false)) {
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
                String template = keyStoreConfiguration.getString("template").orElse(null);
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
