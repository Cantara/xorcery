package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.certificates.KeyStores;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

@Service(name = "server.ssl")
public class ServerSslContextFactoryFactory
        implements Factory<SslContextFactory.Server>, PreDestroy {
    private final SslContextFactory.Server factory;

    @Inject
    public ServerSslContextFactoryFactory(Configuration configuration, KeyStores keyStores) throws Exception {
        factory = new SslContextFactory.Server();
        factory.setKeyStore(keyStores.getKeyStore("certificates.keystore"));
        factory.setTrustStore(keyStores.getKeyStore("certificates.truststore"));
        factory.setKeyManagerPassword(configuration.getString("certificates.keystore.password").orElse(null));
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
