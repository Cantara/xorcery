package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.*;
import java.io.IOException;
import java.net.InetAddress;

@Service
@Named("registry.dnsdiscovery")
public class RegistryDnsDiscovery
        implements ServiceListener, ServiceTypeListener, PreDestroy {

    private Logger logger = LogManager.getLogger(getClass());
    private JmDNS jmdns;
    private Configuration configuration;
    private Topic<ServiceResourceObject> resourceObjectTopic;

    @Inject
    public RegistryDnsDiscovery(Configuration configuration, Topic<ServiceResourceObject> resourceObjectTopic) {
        this.configuration = configuration;
        this.resourceObjectTopic = resourceObjectTopic;

        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.addServiceTypeListener(this);
            jmdns.addServiceListener("_https._tcp.local.", this);
            ServiceInfo serviceInfo = ServiceInfo.create("_https._tcp.local.", configuration.getString("id").orElse("xorcery"), configuration.getInteger("server.ssl.port").orElse(443), "path=/api/registry");
            jmdns.registerService(serviceInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preDestroy() {
        jmdns.unregisterAllServices();
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
