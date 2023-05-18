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
