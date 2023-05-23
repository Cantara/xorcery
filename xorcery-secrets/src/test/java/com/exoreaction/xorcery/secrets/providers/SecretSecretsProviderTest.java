package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.secrets.Secrets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SecretSecretsProviderTest {

    @Test
    public void getSecretStringTest() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .build();

        try (Xorcery xorcery = new Xorcery(configuration)) {

            Secrets secrets = xorcery.getServiceLocator().getService(Secrets.class);
            Assertions.assertEquals("foobar", secrets.getSecretString("secret:foobar"));
        }
    }
}
