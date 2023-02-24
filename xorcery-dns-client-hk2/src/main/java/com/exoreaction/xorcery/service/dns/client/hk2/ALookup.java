package com.exoreaction.xorcery.service.dns.client.hk2;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.lookup.LookupSession;

@Service
@ContractsProvided({DnsLookup.class})
@Rank(3)
public class ALookup
    extends com.exoreaction.xorcery.service.dns.client.ALookup
{
    @Inject
    public ALookup(LookupSession lookupSession) {
        super(lookupSession);
    }
}
