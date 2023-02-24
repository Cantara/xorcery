package com.exoreaction.xorcery.service.dns.client.hk2;


import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service()
@ContractsProvided({DnsLookup.class})
@Rank(1)
public class JavaLookup
    extends com.exoreaction.xorcery.service.dns.client.JavaLookup
{
    @Inject
    public JavaLookup() {
        super();
    }
}
