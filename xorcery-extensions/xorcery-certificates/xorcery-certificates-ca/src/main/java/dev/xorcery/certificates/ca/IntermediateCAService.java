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
package dev.xorcery.certificates.ca;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.jsonapi.service.ServiceResourceObject;
import dev.xorcery.jsonapi.service.ServiceResourceObjects;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "intermediateca")
@RunLevel(4)
public class IntermediateCAService {

    @Inject
    public IntermediateCAService(ServiceResourceObjects sro, Configuration configuration) {
        sro.add(new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), "intermediateca")
                .version(getClass().getPackage().getImplementationVersion())
                .with(b ->
                {
                    b.api("certificate", "api/ca/rootca.cer");
                    b.api("crl", "api/ca/intermediateca.crl");
                })
                .build());
    }
}
