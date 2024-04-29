package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Note: this is set to run level 0 primarily so that client streams are the last to close when an application shuts down.
 */
@Service(name = "reactivestreams.client.reactor")
@ContractsProvided({ClientWebSocketStreamsClient.class})
@RunLevel(0)
public class ClientWebSocketStreamsServiceHK2
    extends ClientWebSocketStreamsService
{
    @Inject
    public ClientWebSocketStreamsServiceHK2(Configuration configuration, MessageWorkers messageWorkers, HttpClient httpClient, DnsLookupService dnsLookup, OpenTelemetry openTelemetry, LoggerContext loggerContext) throws Exception {
        super(configuration, messageWorkers, httpClient, dnsLookup, openTelemetry, loggerContext);
    }
}
