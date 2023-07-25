package com.exoreaction.xorcery.keystores.providers;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.test.XorceryExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class KeyStoreSecretsProviderTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void getSecretStringTest() throws Exception {
        Secrets secrets = xorcery.getXorcery().getServiceLocator().getService(Secrets.class);
        Assertions.assertEquals("mysecretpassword", secrets.getSecretString("keystore:secretpassword"));
    }
}
