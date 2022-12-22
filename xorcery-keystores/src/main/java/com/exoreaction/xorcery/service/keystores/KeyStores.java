package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service(name = "keystores")
public class KeyStores {
    private final Configuration configuration;
    private final Topic<KeyStore> keyStoreTopic;
    private final Map<String, KeyStore> keyStores = new ConcurrentHashMap<>();

    @Inject
    public KeyStores(Configuration configuration, Topic<KeyStore> keyStoreTopic) {
        this.configuration = configuration;
        this.keyStoreTopic = keyStoreTopic;

        Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        if (null == Security.getProvider(p.getName())) {
            Security.addProvider(p);
        }
    }

    public KeyStore getKeyStore(String configurationPrefix) {
        return keyStores.computeIfAbsent(configurationPrefix, keyStoreName ->
        {
            URL keyStoreUrl = null;
            try {
                Configuration keyStoreConfiguration = configuration.getConfiguration(keyStoreName);
                KeyStore keyStore = KeyStore.getInstance(keyStoreConfiguration.getString("type").orElse("PKCS12"));
                keyStoreUrl = keyStoreConfiguration.getResourceURL("path").orElseThrow(() -> new IllegalArgumentException("Missing " + keyStoreName + ".path"));
                try (InputStream inStream = keyStoreUrl.openStream()) {
                    keyStore.load(inStream, keyStoreConfiguration.getString("password").map(String::toCharArray).orElse(null));

                    if (keyStoreConfiguration.getBoolean("loadrootca").orElse(false)) {
                        addDefaultRootCaCertificates(keyStore);
                    }

                    LogManager.getLogger(getClass()).info("Loaded keystore " + configurationPrefix+"("+keyStoreUrl+")");
                    {
                        StringBuilder builder = new StringBuilder();
                        keyStore.aliases().asIterator().forEachRemaining(alias -> builder.append(alias).append("\n"));
                        LogManager.getLogger(getClass()).debug(keyStoreName + " aliases:\n" + builder);
                    }

                    return keyStore;
                }
            } catch (Throwable e) {
                throw new RuntimeException("Could not load keystore "+keyStoreName+" from "+keyStoreUrl, e);
            }
        });
    }

    public KeyStore getDefaultKeyStore()
    {
        return getKeyStore("keystores.keystore");
    }

    public KeyStore getDefaultTrustStore()
    {
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
                keyStoreTopic.publish(keyStore);
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
}
