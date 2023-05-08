package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.lookup.LookupSession;

@Service
public class DnsLookupSessionFactoryHK2
        extends com.exoreaction.xorcery.service.dns.client.DnsLookupSessionFactory
        implements Factory<LookupSession> {
    @Inject
    public DnsLookupSessionFactoryHK2(Configuration configuration) {
        super(configuration);
    }

    @Override
    @Singleton
    public LookupSession provide() {
        return super.provide();
    }
}
