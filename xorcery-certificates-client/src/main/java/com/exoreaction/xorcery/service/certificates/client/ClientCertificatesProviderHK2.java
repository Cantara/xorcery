package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import jakarta.inject.Inject;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "certificates.client")
@RunLevel(value = 2)
@ContractsProvided(CertificatesProvider.class)
public class ClientCertificatesProviderHK2
        extends ClientCertificatesProvider {
    @Inject
    public ClientCertificatesProviderHK2(HttpClient httpClient,
                                         DnsLookupService dnsLookupService,
                                         Configuration configuration) {
        super(httpClient, dnsLookupService, configuration);
    }
}
