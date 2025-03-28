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
package dev.xorcery.certificates.provisioning;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.keystores.KeyStores;
import dev.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import org.bouncycastle.operator.OperatorCreationException;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service(name = "certificates")
@RunLevel(value = 2)
@ContractsProvided(CertificateProvisioningService.class)
public class CertificateProvisioningServiceHK2
        extends CertificateProvisioningService
        implements PreDestroy {
    @Inject
    public CertificateProvisioningServiceHK2(ServiceLocator serviceLocator,
                                             KeyStores keyStores,
                                             Secrets secrets,
                                             Configuration configuration) throws GeneralSecurityException, IOException, OperatorCreationException {
        super(serviceLocator.create(RequestCertificateProcess.Factory.class), keyStores, secrets, configuration);
    }

    @Override
    public void preDestroy() {
        close();
    }
}
