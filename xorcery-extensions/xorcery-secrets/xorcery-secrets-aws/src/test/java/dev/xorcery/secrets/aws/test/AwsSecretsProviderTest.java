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