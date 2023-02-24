package com.exoreaction.xorcery.service.dns.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.client.hosts", metadata = "enabled=dns.client.hosts")
@ContractsProvided({DnsLookup.class})
@Rank(5)

public class HostsConfigurationLookup
    extends com.exoreaction.xorcery.service.dns.client.HostsConfigurationLookup
{

    @Inject
    public HostsConfigurationLookup(Configuration configuration) {
        super(configuration);
    }
}
