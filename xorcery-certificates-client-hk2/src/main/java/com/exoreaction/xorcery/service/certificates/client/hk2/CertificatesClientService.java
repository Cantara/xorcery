package com.exoreaction.xorcery.service.certificates.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

@Service(name = "certificates.client")
@RunLevel(value = 2)
public class CertificatesClientService
    extends com.exoreaction.xorcery.service.certificates.client.CertificatesClientService
{
    @Inject
    public CertificatesClientService(KeyStores keyStores,
                                     HttpClient httpClient,
                                     DnsLookupService dnsLookupService,
                                     Configuration configuration) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        super(keyStores, httpClient, dnsLookupService, configuration);
    }
}
