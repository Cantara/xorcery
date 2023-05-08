package com.exoreaction.xorcery.service.dns.client.discovery;

import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import java.io.IOException;

@Service(name = "dns.client.discovery")
@RunLevel(2)
public class DnsDiscoveryServiceHK2
    extends com.exoreaction.xorcery.service.dns.client.discovery.DnsDiscoveryService
{
    @Inject
    public DnsDiscoveryServiceHK2(JmDNS jmDNS) throws IOException {
        super(jmDNS);
    }
}
