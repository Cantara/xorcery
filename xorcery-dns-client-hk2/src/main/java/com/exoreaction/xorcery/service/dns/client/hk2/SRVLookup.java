package com.exoreaction.xorcery.service.dns.client.hk2;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.lookup.LookupSession;

@Service
@ContractsProvided({DnsLookup.class})
@Rank(4)
public class SRVLookup
        extends com.exoreaction.xorcery.service.dns.client.SRVLookup {

    @Inject
    public SRVLookup(LookupSession lookupSession) {
        super(lookupSession);
    }
}
