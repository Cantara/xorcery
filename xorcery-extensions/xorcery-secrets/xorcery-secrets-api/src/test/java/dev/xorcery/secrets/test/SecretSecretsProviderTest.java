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
package dev.xorcery.secrets.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.secrets.Secrets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SecretSecretsProviderTest {

    @Test
    public void getSecretStringTest() throws Exception {
        Configuration configuration = new ConfigurationBuilder()
                .addTestDefaults()
                .build();

        try (Xorcery xorcery = new Xorcery(configuration)) {

            Secrets secrets = xorcery.getServiceLocator().getService(Secrets.class);
            Assertions.assertEquals("foobar", secrets.getSecretString("secret:foobar"));
        }
    }
}
