package dev.xorcery.reactivestreams.client;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.dns.client.providers.DnsLookupService;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.spi.MessageWorkers;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Note: this is set to run level 0 primarily so that client streams are the last to close when an application shuts down.
 */
@Service(name = "reactivestreams.client")
@ContractsProvided({ClientWebSocketStreams.class})
@RunLevel(0)
public class ClientWebSocketStreamsServiceHK2
    extends ClientWebSocketStreamsService
    implements PreDestroy
{
    @Inject
    public ClientWebSocketStreamsServiceHK2(Configuration configuration, MessageWorkers messageWorkers, HttpClient httpClient, DnsLookupService dnsLookup, OpenTelemetry openTelemetry, LoggerContext loggerContext) throws Exception {
        super(configuration, messageWorkers, httpClient, dnsLookup, openTelemetry, loggerContext);
    }
}
