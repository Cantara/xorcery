package dev.xorcery.jetty.server.http2.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.jetty.server.JettyServerConfiguration;
import dev.xorcery.jetty.server.JettyServerSslConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.http2")
public class Http2ConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{
    private final HTTP2ServerConnectionFactory h2;

    @Inject
    public Http2ConnectionFactoryFactory(Configuration configuration) {
        JettyServerConfiguration jettyConfig = JettyServerConfiguration.get(configuration);
        JettyServerSslConfiguration sslConfig = JettyServerSslConfiguration.get(configuration);

        final HttpConfiguration sslHttpConfig = new HttpConfiguration();
        sslHttpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        sslHttpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniRequired(sslConfig.isSniRequired());
        customizer.setSniHostCheck(sslConfig.isSniHostCheck());
        sslHttpConfig.addCustomizer(customizer);

        h2 = new HTTP2ServerConnectionFactory(sslHttpConfig);
    }

    @Override
    @Named("h2")
    @Singleton
    public ConnectionFactory provide() {
        return h2;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
