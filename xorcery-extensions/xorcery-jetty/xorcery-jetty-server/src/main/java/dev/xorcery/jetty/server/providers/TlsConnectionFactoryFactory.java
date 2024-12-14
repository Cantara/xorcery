package dev.xorcery.jetty.server.providers;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.ssl")
public class TlsConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{
    private final SslConnectionFactory tls;

    @Inject
    public TlsConnectionFactoryFactory(
            SslContextFactory.Server sslContextFactory,
            @Optional @Named("alpn") ConnectionFactory alpn
    ) {
        tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
    }

    @Override
    @Named("tls")
    @Singleton
    public ConnectionFactory provide() {
        return tls;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
