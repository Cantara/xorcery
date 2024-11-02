/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.dns.multicast;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

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

@Service(name = "dns.multicast.discovery")
public class DnsDiscoveryService
        implements ServiceListener, ServiceTypeListener {

    private final Logger logger = LogManager.getLogger(getClass());
    private final JmDNS jmdns;

    private final Map<String, List<ServiceEvent>> services = new ConcurrentHashMap<>();

    @Inject
    public DnsDiscoveryService(JmDNS jmDNS) throws IOException {
        jmdns = jmDNS;
        jmdns.addServiceTypeListener(this);
    }

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        logger.debug("Added service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
        services.computeIfAbsent(serviceEvent.getType(), type -> new ArrayList<>()).add(serviceEvent);
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {
        logger.debug("Removed service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
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
        logger.debug("Resolved service:" + serviceEvent.getName() + ":" + serviceEvent.getType() + ":" + serviceEvent.getInfo());
    }

    @Override
    public void serviceTypeAdded(ServiceEvent event) {
        logger.debug("Service type added:" + event.getType());
        jmdns.addServiceListener(event.getType(), this);
    }

    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {
        logger.debug("Service sub type added:" + event.getType());
        jmdns.addServiceListener(event.getType(), this);
    }

    public Map<String, List<ServiceEvent>> getServices() {
        return services;
    }
}
