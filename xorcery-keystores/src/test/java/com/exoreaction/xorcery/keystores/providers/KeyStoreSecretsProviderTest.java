package com.exoreaction.xorcery.keystores.providers;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.secrets.Secrets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KeyStoreSecretsProviderTest {

    @Test
    public void getSecretStringTest() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .build();

        System.out.println(configuration);

        try (Xorcery xorcery = new Xorcery(configuration)) {

            Secrets secrets = xorcery.getServiceLocator().getService(Secrets.class);
            Assertions.assertEquals("mysecretpassword", secrets.getSecretString("keystore:secretpassword"));
        }
    }
}
