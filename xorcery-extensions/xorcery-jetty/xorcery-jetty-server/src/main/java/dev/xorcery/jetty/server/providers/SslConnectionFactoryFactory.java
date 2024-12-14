package dev.xorcery.jetty.server.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.jetty.server.JettyServerConfiguration;
import dev.xorcery.jetty.server.JettyServerSslConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.*;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server.ssl")
public class SslConnectionFactoryFactory
        implements Factory<ConnectionFactory> {

    private final HttpConnectionFactory sslHttp11;

    @Inject
    public SslConnectionFactoryFactory(Configuration configuration) {
        JettyServerConfiguration jettyConfig = JettyServerConfiguration.get(configuration);
        JettyServerSslConfiguration sslConfig = JettyServerSslConfiguration.get(configuration);

        final HttpConfiguration sslHttpConfig = new HttpConfiguration();
        sslHttpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        sslHttpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniRequired(sslConfig.isSniRequired());
        customizer.setSniHostCheck(sslConfig.isSniHostCheck());
        sslHttpConfig.addCustomizer(customizer);

        // Added for X-Forwarded-For support, from ALB
        sslHttpConfig.addCustomizer(new ForwardedRequestCustomizer());

        sslHttp11 = new HttpConnectionFactory(sslHttpConfig);
    }

    @Override
    @Named("ssl")
    @Singleton
    public ConnectionFactory provide() {
        return sslHttp11;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
