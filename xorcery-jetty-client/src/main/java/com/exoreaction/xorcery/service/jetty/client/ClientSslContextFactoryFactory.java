package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.security.KeyStore;
import java.util.Optional;

import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

public class ClientSslContextFactoryFactory {
    private final SslContextFactory.Client factory;

    public ClientSslContextFactoryFactory(Configuration configuration, Optional<KeyStores> keyStores) throws Exception {
        factory = new SslContextFactory.Client();
        keyStores.ifPresent(ks -> {
            factory.setKeyStore(ks.getKeyStore("keystores.keystore"));
            factory.setTrustStore(ks.getKeyStore("keystores.truststore"));
            factory.setKeyManagerPassword(configuration.getString("keystores.keystore.password").orElse(null));
            factory.setCertAlias(configuration.getString("jetty.client.ssl.alias").orElse("self"));
        });
        factory.setEndpointIdentificationAlgorithm("HTTPS");
        factory.setHostnameVerifier((hostName, session) -> true);
        factory.setTrustAll(configuration.getBoolean("jetty.client.ssl.trustall").orElse(false));
        factory.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);
        factory.start();
    }

    public void preDestroy() {
        try {
            factory.stop();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not stop SSL context factory", e);
        }
    }

    public SslContextFactory.Client provide() {
        return factory;
    }


    public void keyStoreUpdated(KeyStore updatedKeyStore) {
        try {
            factory.reload(scf ->
                    LogManager.getLogger(getClass()).info("Reloaded client keystore"));
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Error reloading client keystore", e);
        }
    }
}
