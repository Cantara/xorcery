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
package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class KeyStoresTest {

    String config = """
            keystores:
              enabled: "{{ defaults.enabled }}"
              teststore:
                path: "{{ instance.home }}/target/teststore.p12"
                template: "META-INF/teststore.p12"
                password: "password"
              emptystore:
                path: "{{ instance.home }}/target/emptystore.p12"
                password: "password"
                        """;

    @Test
    public void testKeyStoreTemplate() throws NoSuchAlgorithmException, NoSuchProviderException {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .build();
        KeyStores keyStores = new KeyStores(configuration);

        // When
        KeyStore keyStore = keyStores.getKeyStore("teststore");

        // Then
        assertThat(keyStore, notNullValue());
    }

    @Test
    public void testCreateEmptyKeyStore() throws NoSuchAlgorithmException, NoSuchProviderException {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .build();
        KeyStores keyStores = new KeyStores(configuration);

        // When
        KeyStore keyStore = keyStores.getKeyStore("emptystore");

        // Then
        assertThat(keyStore, notNullValue());
    }
}