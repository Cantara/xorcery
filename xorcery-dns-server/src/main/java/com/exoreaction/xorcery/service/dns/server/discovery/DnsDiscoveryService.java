package com.exoreaction.xorcery.service.dns.server.discovery;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import java.io.IOException;

@Service(name = "dns.server.discovery")
@RunLevel(2)
public class DnsDiscoveryService
        implements ServiceListener, ServiceTypeListener {

    private final Logger logger = LogManager.getLogger(getClass());
    private final JmDNS jmdns;
    private Configuration configuration;

    @Inject
    public DnsDiscoveryService(Configuration configuration,
                               JmDNS jmDNS) throws IOException {
        this.configuration = configuration;

        jmdns = jmDNS;
        jmdns.addServiceTypeListener(this);
/*
            jmdns.addServiceListener("_https._tcp.local.", this);
            jmdns.addServiceListener("_http._tcp.local.", this);
*/
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
        jmdns.addServiceListener(event.getType(), this);
    }

    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {
        logger.info("Service sub type added:" + event.getType());
        jmdns.addServiceListener(event.getType(), this);
    }
}
