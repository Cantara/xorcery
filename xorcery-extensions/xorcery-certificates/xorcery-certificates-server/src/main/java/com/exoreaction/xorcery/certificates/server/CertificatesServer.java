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
package com.exoreaction.xorcery.certificates.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.jsonapi.service.ServiceResourceObject;
import com.exoreaction.xorcery.jsonapi.service.ServiceResourceObjects;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "certificates.server")
@RunLevel(value = 2)
public class CertificatesServer {

    @Inject
    public CertificatesServer(ServiceResourceObjects serviceResourceObjects,
                              Configuration configuration
    ) {
        serviceResourceObjects.add(new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), "certificates")
                .version(getClass().getPackage().getImplementationVersion())
                .with(b ->
                {
                    b.api("request", "api/certificates/request");
                })
                .build());
    }
}
