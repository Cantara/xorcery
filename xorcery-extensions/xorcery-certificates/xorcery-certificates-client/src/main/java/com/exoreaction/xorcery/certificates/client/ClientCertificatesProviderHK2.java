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

import com.exoreaction.xorcery.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "certificates.client")
@ContractsProvided(CertificatesProvider.class)
public class ClientCertificatesProviderHK2
        extends ClientCertificatesProvider
        implements PreDestroy {
    @Inject
    public ClientCertificatesProviderHK2(ClientBuilder clientBuilder,
                                         Configuration configuration) {
        super(clientBuilder, configuration);
    }

    @Override
    public void preDestroy() {
        close();
    }
}
