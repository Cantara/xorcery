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
package com.exoreaction.xorcery.service.certificates.ca.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.certificates.ca.IntermediateCACertificatesProvider;
import org.junit.jupiter.api.Test;

public class GenerateEmptyCRLTest {

    String config = """
            certificates.server.self.enabled: false
            keystores:
              keystore:
                path: "META-INF/intermediatecakeystore.p12"
              truststore:
                path: "META-INF/intermediatecatruststore.p12"
            """;

    @Test
    public void testGenerateEmptyCRL() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));
        try (Xorcery xorcery = new Xorcery(configuration))
        {
            String crls = xorcery.getServiceLocator().getService(IntermediateCACertificatesProvider.class).getCRL();
            System.out.println(crls);
        }
    }
}
