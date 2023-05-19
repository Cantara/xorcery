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
package com.exoreaction.xorcery.service.dns.client.discovery;

import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.JmDNS;
import java.io.IOException;

@Service(name = "dns.client.discovery")
@RunLevel(2)
public class DnsDiscoveryServiceHK2
    extends com.exoreaction.xorcery.service.dns.client.discovery.DnsDiscoveryService
{
    @Inject
    public DnsDiscoveryServiceHK2(JmDNS jmDNS) throws IOException {
        super(jmDNS);
    }
}
