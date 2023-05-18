package com.exoreaction.xorcery.service.certificates;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import org.bouncycastle.operator.OperatorCreationException;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.*;

@Service(name = "certificates")
@RunLevel(value = 2)
@ContractsProvided(CertificatesService.class)
public class CertificatesServiceHK2
        extends CertificatesService
        implements PreDestroy {
    @Inject
    public CertificatesServiceHK2(ServiceLocator serviceLocator,
                                  KeyStores keyStores,
                                  Configuration configuration) throws GeneralSecurityException, IOException, OperatorCreationException {
        super(serviceLocator.create(RequestCertificateProcess.Factory.class), keyStores, configuration);
    }

    @Override
    public void preDestroy() {
        close();
    }
}
