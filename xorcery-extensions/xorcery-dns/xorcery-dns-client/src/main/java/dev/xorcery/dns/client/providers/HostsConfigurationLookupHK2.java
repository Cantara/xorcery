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
package dev.xorcery.dns.client.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.client.hosts", metadata = "enabled=dns.client.hosts")
@ContractsProvided({DnsLookup.class})
@Rank(5)

public class HostsConfigurationLookupHK2
    extends HostsConfigurationLookup
{

    @Inject
    public HostsConfigurationLookupHK2(Configuration configuration) {
        super(configuration);
    }
}
