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
package com.exoreaction.xorcery.jersey.client.providers;

import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

public class SRVConnectorProvider
        implements ConnectorProvider {
    private final DnsLookup dnsLookup;
    private String scheme;
    private ConnectorProvider delegateProvider;

    public SRVConnectorProvider(DnsLookupService dnsLookup, String scheme, ConnectorProvider delegateProvider) {
        this.dnsLookup = dnsLookup;
        this.scheme = scheme;
        this.delegateProvider = delegateProvider;
    }

    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig) {
        return new SRVConnector(dnsLookup, scheme, delegateProvider.getConnector(client, runtimeConfig));
    }
}
