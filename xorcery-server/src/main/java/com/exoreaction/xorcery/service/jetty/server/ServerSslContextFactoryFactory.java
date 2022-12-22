package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

@Service(name = "server.ssl")
public class ServerSslContextFactoryFactory
        implements Factory<SslContextFactory.Server>, PreDestroy {
    private final SslContextFactory.Server factory;

    @Inject
    public ServerSslContextFactoryFactory(Configuration configuration, KeyStores keyStores) throws Exception {
        factory = new SslContextFactory.Server();
        factory.setKeyStore(keyStores.getKeyStore("keystores.keystore"));
        factory.setTrustStore(keyStores.getKeyStore("keystores.truststore"));
        factory.setKeyManagerPassword(configuration.getString("keystores.keystore.password").orElse(null));
        factory.setCertAlias(configuration.getString("server.ssl.alias").orElse("self"));
        factory.setHostnameVerifier((hostName, session) -> true);
        factory.setTrustAll(configuration.getBoolean("server.ssl.trustall").orElse(false));
        factory.setNeedClientAuth(configuration.getBoolean("server.ssl.needclientauth").orElse(false));
        factory.setWantClientAuth(configuration.getBoolean("server.ssl.wantclientauth").orElse(false));
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
    public SslContextFactory.Server provide() {
        return factory;
    }

    @Override
    public void dispose(SslContextFactory.Server instance) {
    }
}
