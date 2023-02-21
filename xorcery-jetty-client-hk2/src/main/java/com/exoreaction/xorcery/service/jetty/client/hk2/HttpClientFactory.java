package com.exoreaction.xorcery.service.jetty.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.client")
public class HttpClientFactory extends com.exoreaction.xorcery.service.jetty.client.HttpClientFactory
        implements Factory<HttpClient>, PreDestroy {

    @Inject
    public HttpClientFactory(Configuration configuration, Provider<DnsLookup> dnsLookup, Provider<SslContextFactory.Client> clientSslContextFactoryProvider) throws Exception {
        super(configuration, dnsLookup::get, clientSslContextFactoryProvider::get);
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
    }

    @Override
    @Singleton
    public HttpClient provide() {
        return super.provide();
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
