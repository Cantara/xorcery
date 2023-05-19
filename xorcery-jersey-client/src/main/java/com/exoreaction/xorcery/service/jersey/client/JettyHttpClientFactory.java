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
package com.exoreaction.xorcery.service.jersey.client;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;

@Singleton
public class JettyHttpClientFactory
        implements Factory<JettyHttpClientContract> {

    private HttpClient client;

    @Inject
    public JettyHttpClientFactory(HttpClient client) {
        this.client = client;
    }

    @Override
    @Singleton
    public JettyHttpClientContract provide() {
        return new JettyHttpClientSupplier(client);
    }

    @Override
    public void dispose(JettyHttpClientContract instance) {
    }
}
