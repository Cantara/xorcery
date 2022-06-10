package com.exoreaction.reactiveservices.service.registry;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.jmdns.*;
import java.io.IOException;
import java.net.InetAddress;

@Singleton
public class RegistryDnsDiscovery
        implements ContainerLifecycleListener, ServiceListener, ServiceTypeListener {

    private Logger logger = LogManager.getLogger(getClass());
    private JmDNS jmdns;
    private Registry registry;
    private Configuration configuration;

    @Inject
    public RegistryDnsDiscovery(Registry registry, Configuration configuration) {
        this.registry = registry;
        this.configuration = configuration;
    }

    @Override
    public void onStartup(Container container) {
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.addServiceTypeListener(this);
            jmdns.addServiceListener("_https._tcp.local.", this);
            ServiceInfo serviceInfo = ServiceInfo.create("_https._tcp.local.", configuration.getString("name").orElse("server"), configuration.getInteger("server.secure_port").orElse(443), "path=/api/registry");
            jmdns.registerService(serviceInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
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
