package com.exoreaction.xorcery.service.dns.client.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DnsDiscoveryService
        implements ServiceListener, ServiceTypeListener {

    private final Logger logger = LogManager.getLogger(getClass());
    private final JmDNS jmdns;

    private final Map<String, List<ServiceEvent>> services = new ConcurrentHashMap<>();

    public DnsDiscoveryService(JmDNS jmDNS) throws IOException {
        jmdns = jmDNS;
        jmdns.addServiceTypeListener(this);
    }

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        logger.info("Added service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
        services.computeIfAbsent(serviceEvent.getType(), type -> new ArrayList<>()).add(serviceEvent);
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {
        logger.info("Removed service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
        Optional.ofNullable(services.get(serviceEvent.getType())).ifPresent(list ->
        {
            for (int i = 0; i < list.size(); i++) {
                ServiceEvent event = list.get(i);
                if (event.getName().equals(serviceEvent.getName())) {
                    list.remove(i);
                    return;
                }
            }
        });
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
        logger.info("Resolved service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
    }

    @Override
    public void serviceTypeAdded(ServiceEvent event) {
        logger.info("Service type added:" + event.getType());
        jmdns.addServiceListener(event.getType(), this);
    }

    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {
        logger.info("Service sub type added:" + event.getType());
        jmdns.addServiceListener(event.getType(), this);
    }

    public Map<String, List<ServiceEvent>> getServices() {
        return services;
    }
}
