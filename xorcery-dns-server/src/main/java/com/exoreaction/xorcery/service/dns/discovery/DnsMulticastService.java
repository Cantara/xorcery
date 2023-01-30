package com.exoreaction.xorcery.service.dns.discovery;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

@Service(name = "dns.multicast")
@RunLevel(20)
public class DnsMulticastService
        implements PreDestroy {

    private final String selfName;
    private final Logger logger = LogManager.getLogger(getClass());
    private final JmDNS jmdns;

    @Inject
    public DnsMulticastService(
            Configuration configuration,
            ServiceResourceObjects serviceResourceObjects,
            JmDNS jmDNS) throws IOException {
        jmdns = jmDNS;
        selfName = configuration.getString("id").orElse("xorcery");
        StandardConfiguration standardConfiguration = () -> configuration;
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {

            String type = serviceResource.getServiceIdentifier().resourceObjectIdentifier().getType();
            int weight = configuration.getInteger(type + ".srv.weight").orElse(1);
            int priority = configuration.getInteger(type + ".srv.priority").orElse(1);

            Map<String, Object> properties = new HashMap<>(serviceResource.getAttributes().attributes().toMap());
            for (Link link : serviceResource.resourceObject().getLinks().getLinks()) {
                properties.put(link.rel(), link.getHref());
            }

            ServiceInfo serviceInfo = ServiceInfo.create("_" + type + "._tcp.local.", selfName, standardConfiguration.getServerUri().getPort(), weight, priority, properties);
            jmdns.registerService(serviceInfo);
            logger.debug("Registered DNS service:" + serviceInfo.getNiceTextString());
        }
        configuration.getInteger("server.http.port").ifPresent(port ->
        {

            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", selfName, port, "");
            try {
                jmdns.registerService(serviceInfo);
                logger.debug("Registered DNS service:" + serviceInfo.getNiceTextString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        if (configuration.getBoolean("server.ssl.enabled").orElse(false)) {
            ServiceInfo serviceInfo = ServiceInfo.create("_https._tcp.local.", selfName, standardConfiguration.getServerUri().getPort(), "");
            jmdns.registerService(serviceInfo);
        }
    }

    @Override
    public void preDestroy() {
        jmdns.unregisterAllServices();
    }
}
