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
package com.exoreaction.xorcery.secrets.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.secrets.Secrets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnvSecretsProviderTest {

    @Test
    public void getSecretStringTest() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .build();

        try (Xorcery xorcery = new Xorcery(configuration)) {

            Secrets secrets = xorcery.getServiceLocator().getService(Secrets.class);
            String firstEnvVar = System.getenv().keySet().iterator().next();
            Assertions.assertEquals(System.getenv(firstEnvVar), secrets.getSecretString(String.format("env:%s", firstEnvVar)));
        }
    }
}
