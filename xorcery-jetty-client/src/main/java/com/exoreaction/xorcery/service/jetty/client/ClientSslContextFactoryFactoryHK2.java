package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;
import java.util.Optional;

@Service(name = "jetty.client.ssl")
@MessageReceiver(KeyStore.class)
public class ClientSslContextFactoryFactoryHK2 extends com.exoreaction.xorcery.service.jetty.client.ClientSslContextFactoryFactory
        implements Factory<SslContextFactory.Client>, PreDestroy {

    @Inject
    public ClientSslContextFactoryFactoryHK2(Configuration configuration, Provider<KeyStores> keyStores) throws Exception {
        super(configuration, Optional.ofNullable(keyStores.get()));
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
    }

    @Singleton
    @Named("jetty.client.ssl")
    @Override
    public SslContextFactory.Client provide() {
        return super.provide();
    }

    @Override
    public void dispose(SslContextFactory.Client instance) {
    }

    public void keyStoreUpdated(@SubscribeTo KeyStore updatedKeyStore) {
        super.keyStoreUpdated(updatedKeyStore);
    }
}
