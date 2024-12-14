package dev.xorcery.jetty.server.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.jetty.server.JettyServerConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.http")
public class Http11ConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{
    private final HttpConnectionFactory http11;

    @Inject
    public Http11ConnectionFactoryFactory(Configuration configuration) {
        JettyServerConfiguration jettyConfig = new JettyServerConfiguration(configuration.getConfiguration("jetty.server"));

        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        httpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        // Added for X-Forwarded-For support, from ALB
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        http11 = new HttpConnectionFactory(httpConfig);

    }

    @Override
    @Named("http11")
    @Singleton
    public ConnectionFactory provide() {
        return http11;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
