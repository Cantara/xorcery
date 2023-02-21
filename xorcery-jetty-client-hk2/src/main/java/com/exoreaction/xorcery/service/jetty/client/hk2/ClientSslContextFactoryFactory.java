package com.exoreaction.xorcery.service.jetty.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;

@Service(name = "jetty.client.ssl")
@MessageReceiver(KeyStore.class)
public class ClientSslContextFactoryFactory extends com.exoreaction.xorcery.service.jetty.client.ClientSslContextFactoryFactory
        implements Factory<SslContextFactory.Client>, PreDestroy {

    @Inject
    public ClientSslContextFactoryFactory(Configuration configuration, KeyStores keyStores) throws Exception {
        super(configuration, keyStores);
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
