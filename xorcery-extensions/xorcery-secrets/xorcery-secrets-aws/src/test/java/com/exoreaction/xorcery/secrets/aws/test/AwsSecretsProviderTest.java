package com.exoreaction.xorcery.secrets.aws.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.secrets.Secrets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Disabled
class AwsSecretsProviderTest {

    static String config = """
            aws.region: us-east-1
                        """;

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .build();

    @Test
    public void testGetSecretString()
    {
        Secrets secrets = xorcery.getServiceLocator().getService(Secrets.class);

        String secretValue = secrets.getSecretString("aws:xorcery-secrets-aws/testsecret");
        Assertions.assertEquals("{\"testsecret\":\"secretvalue\"}", secretValue);
    }

    @Test
    public void testRefreshSecret() {
        Secrets secrets = xorcery.getServiceLocator().getService(Secrets.class);

        secrets.refreshSecret("aws:xorcery-secrets-aws/testsecret");
        String secretValue = secrets.getSecretString("aws:xorcery-secrets-aws/testsecret");
        Assertions.assertEquals("{\"testsecret\":\"secretvalue\"}", secretValue);
    }
}