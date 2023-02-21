package com.exoreaction.xorcery.service.reactivestreams.client.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.common.LocalStreamFactories;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "reactivestreams.client")
@ContractsProvided({ReactiveStreamsClient.class})
@RunLevel(8)
public class ReactiveStreamsClientService extends com.exoreaction.xorcery.service.reactivestreams.client.ReactiveStreamsClientService
        implements PreDestroy {

    @Inject
    public ReactiveStreamsClientService(Configuration configuration,
                                        MessageWorkers messageWorkers,
                                        HttpClient httpClient,
                                        DnsLookup dnsLookup,
                                        Provider<LocalStreamFactories> localStreamFactoriesProvider) throws Exception {
        super(configuration, messageWorkers, httpClient, dnsLookup, localStreamFactoriesProvider::get);
    }
}
