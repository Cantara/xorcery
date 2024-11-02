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
package dev.xorcery.certificates.ca.test;

import dev.xorcery.certificates.ca.IntermediateCACertificatesProvider;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

public class GenerateEmptyCRLTest {

    String config = """
            certificates.enabled: false
            """;

    @Test
    public void testGenerateEmptyCRL() throws Exception {
        Configuration configuration = new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml(config)
                .build();
        System.out.println(configuration);
        try (Xorcery xorcery = new Xorcery(configuration))
        {
            String crls = xorcery.getServiceLocator().getService(IntermediateCACertificatesProvider.class).getCRL();
            System.out.println(crls);
        }
    }
}
