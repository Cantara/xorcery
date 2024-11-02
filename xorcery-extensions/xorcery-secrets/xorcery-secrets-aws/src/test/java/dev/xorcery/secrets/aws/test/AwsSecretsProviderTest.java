package dev.xorcery.secrets.aws.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.secrets.Secrets;
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
    public void testGetSecretString(Secrets secrets)
    {
        String secretValue = secrets.getSecretString("aws:xorcery-secrets-aws/testsecret");
        Assertions.assertEquals("{\"testsecret\":\"secretvalue\"}", secretValue);
    }

    @Test
    public void testRefreshSecret(Secrets secrets) {
        secrets.refreshSecret("aws:xorcery-secrets-aws/testsecret");
        String secretValue = secrets.getSecretString("aws:xorcery-secrets-aws/testsecret");
        Assertions.assertEquals("{\"testsecret\":\"secretvalue\"}", secretValue);
    }
}