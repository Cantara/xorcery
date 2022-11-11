package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyWriter;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.jersey.client.ClientConfig;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

@Service
@Named("registry.dnsdiscovery")
public class RegistryDnsDiscovery
        implements ServiceListener, ServiceTypeListener, PreDestroy {

    private final String selfName;
    private final JsonApiClient client;
    private Logger logger = LogManager.getLogger(getClass());
    private JmDNS jmdns;
    private Configuration configuration;
    private Topic<ServerResourceDocument> serverRegistryTopic;

    @Inject
    public RegistryDnsDiscovery(Configuration configuration,
                                Topic<ServerResourceDocument> serverRegistryTopic,
                                ClientConfig clientConfig) {
        this.configuration = configuration;
        this.serverRegistryTopic = serverRegistryTopic;
        Client client = ClientBuilder.newClient(clientConfig
                .register(JsonElementMessageBodyReader.class)
                .register(JsonElementMessageBodyWriter.class));
        this.client = new JsonApiClient(client);

        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.addServiceTypeListener(this);
            jmdns.addServiceListener("_https._tcp.local.", this);
            jmdns.addServiceListener("_http._tcp.local.", this);
            selfName = configuration.getString("id").orElse("xorcery");
            {
                ServiceInfo serviceInfo = ServiceInfo.create("_https._tcp.local.", selfName, configuration.getInteger("server.ssl.port").orElse(443), "path=/api/registry");
                jmdns.registerService(serviceInfo);
            }
            {
                ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", selfName, configuration.getInteger("server.port").orElse(80), "path=/api/registry");
                jmdns.registerService(serviceInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preDestroy() {
        try {
            jmdns.close();
        } catch (IOException e) {
            logger.warn("Could not shutdown DNS service", e);
        }
    }

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        logger.info("Added service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {
        logger.info("Removed service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
        logger.info("Resolved service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());

        if (serviceEvent.getType().equals("_http._tcp.local."))
            if (!serviceEvent.getName().equals(selfName) && serviceEvent.getInfo().getPropertyString("path").equals("/api/registry")) {
                for (Inet4Address hostAddress : serviceEvent.getInfo().getInet4Addresses()) {
                    Link self = new Link("self", "http://" + hostAddress.getHostAddress() + ":" + serviceEvent.getInfo().getPort());
                    client.get(self)
                            .whenComplete((rd, t) ->
                            {
                                if (t != null) {
                                    logger.error("Could not get server resource document from " + self.getHref(), t);
                                } else {
                                    serverRegistryTopic.publish(new ServerResourceDocument(rd));
                                }
                            });
                }
            }
    }

    @Override
    public void serviceTypeAdded(ServiceEvent event) {
        logger.info("Service type added:" + event.getType());
    }

    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {
        logger.info("Service sub type added:" + event.getType());
    }
}
