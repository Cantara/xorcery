package com.exoreaction.xorcery.service.dns.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.lookup.LookupSession;

@Service
public class DnsLookupSessionFactory
        extends com.exoreaction.xorcery.service.dns.client.DnsLookupSessionFactory
        implements Factory<LookupSession> {
    @Inject
    public DnsLookupSessionFactory(Configuration configuration) {
        super(configuration);
    }

    @Override
    @Singleton
    public LookupSession provide() {
        return super.provide();
    }
}
