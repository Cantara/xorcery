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
package com.exoreaction.xorcery.keystores;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.test.XorceryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class KeyStoresTest {


    static String config = """
            keystores:
              enabled: "{{ defaults.enabled }}"
              teststore:
                path: "{{ instance.home }}/teststore.p12"
                template: "META-INF/teststore.p12"
                password: "{{ keystores.defaultPassword }}"
              emptystore:
                path: "{{ instance.home }}/emptystore.p12"
                password: "{{ keystores.defaultPassword }}"
                        """;

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .build();

    @Test
    public void testKeyStoreTemplate() throws Exception {

        // Given
        KeyStores keyStores = xorcery.getXorcery().getServiceLocator().getService(KeyStores.class);

        // When
        KeyStore keyStore = keyStores.getKeyStore("teststore");

        // Then
        assertThat(keyStore, notNullValue());
    }

    @Test
    public void testCreateEmptyKeyStore() throws Exception {
        // Given
        KeyStores keyStores = xorcery.getXorcery().getServiceLocator().getService(KeyStores.class);

        // When
        KeyStore keyStore = keyStores.getKeyStore("emptystore");

        // Then
        assertThat(keyStore, notNullValue());
    }
}