package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class KeyStoresTest {

    String config = "keystores:\n" +
                    "  enabled: \"{{ defaults.enabled }}\"\n" +
                    "  teststore:\n" +
                    "    path: \"{{ home }}/target/test-classes/META-INF/teststore.p12\"\n" +
                    "    template: \"META-INF/teststore.p12\"\n" +
                    "    password: \"password\"\n";

    @Test
    public void testKeyStoreTemplate() {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .build();
        KeyStores keyStores = new KeyStores(configuration);

        // When
        KeyStore keyStore = keyStores.getKeyStore("keystores.teststore");

        // Then
        assertThat(keyStore, notNullValue());
    }
}