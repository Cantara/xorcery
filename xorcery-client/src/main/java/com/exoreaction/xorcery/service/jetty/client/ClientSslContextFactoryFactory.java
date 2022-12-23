package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;

import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

@Service(name = "client.ssl")
@MessageReceiver(KeyStore.class)
public class ClientSslContextFactoryFactory
        implements Factory<SslContextFactory.Client>, PreDestroy {
    private final SslContextFactory.Client factory;

    @Inject
    public ClientSslContextFactoryFactory(Configuration configuration, KeyStores keyStores) throws Exception {
        factory = new SslContextFactory.Client();
        factory.setKeyStore(keyStores.getKeyStore("keystores.keystore"));
        factory.setTrustStore(keyStores.getKeyStore("keystores.truststore"));
        factory.setKeyManagerPassword(configuration.getString("keystores.keystore.password").orElse(null));
        factory.setCertAlias(configuration.getString("client.ssl.keystore.alias").orElse("self"));
        factory.setEndpointIdentificationAlgorithm("HTTPS");
        factory.setHostnameVerifier((hostName, session) -> true);
        factory.setTrustAll(configuration.getBoolean("client.ssl.trustall").orElse(false));
        factory.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);
        factory.start();
    }

    @Override
    public void preDestroy() {
        try {
            factory.stop();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not stop SSL context factory", e);
        }
    }

    @Singleton
    @Override
    public SslContextFactory.Client provide() {
        return factory;
    }

    @Override
    public void dispose(SslContextFactory.Client instance) {
    }

    public void keyStoreUpdated(@SubscribeTo KeyStore updatedKeyStore) {
        try {
            factory.reload(scf ->
                    LogManager.getLogger(getClass()).info("Reloaded client keystore"));
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Error reloading client keystore", e);
        }
    }
}
