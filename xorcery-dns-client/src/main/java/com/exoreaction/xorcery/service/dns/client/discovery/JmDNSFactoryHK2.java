package com.exoreaction.xorcery.service.dns.client.discovery;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import java.io.IOException;

@Service
public class JmDNSFactoryHK2
        extends com.exoreaction.xorcery.service.dns.client.discovery.JmDNSFactory
        implements Factory<JmDNS> {
    @Inject
    public JmDNSFactoryHK2(Configuration configuration) throws IOException {
        super(configuration);
    }

    @Override
    @Singleton
    public JmDNS provide() {
        return super.provide();
    }
}
