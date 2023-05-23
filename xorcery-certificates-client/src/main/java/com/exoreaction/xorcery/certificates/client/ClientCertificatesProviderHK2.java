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
package com.exoreaction.xorcery.certificates.client;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import jakarta.inject.Inject;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "certificates.client")
@RunLevel(value = 2)
@ContractsProvided(CertificatesProvider.class)
public class ClientCertificatesProviderHK2
        extends ClientCertificatesProvider {
    @Inject
    public ClientCertificatesProviderHK2(HttpClient httpClient,
                                         DnsLookupService dnsLookupService,
                                         Configuration configuration) {
        super(httpClient, dnsLookupService, configuration);
    }
}
