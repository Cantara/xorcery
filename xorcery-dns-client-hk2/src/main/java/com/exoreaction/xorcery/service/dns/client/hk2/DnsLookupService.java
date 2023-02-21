package com.exoreaction.xorcery.service.dns.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.client")
@ContractsProvided({DnsLookup.class})
public class DnsLookupService extends com.exoreaction.xorcery.service.dns.client.DnsLookupService {

    @Inject
    public DnsLookupService(Configuration configuration) {
        super(configuration);
    }
}
