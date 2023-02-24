package com.exoreaction.xorcery.service.dns.client.hk2.discovery;

import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import java.io.IOException;

@Service(name = "dns.discovery")
@RunLevel(2)
public class DnsDiscoveryService
    extends com.exoreaction.xorcery.service.dns.client.discovery.DnsDiscoveryService
{
    @Inject
    public DnsDiscoveryService(JmDNS jmDNS) throws IOException {
        super(jmDNS);
    }
}
