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
package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.client")
public class HttpClientFactoryHK2 extends HttpClientFactory
        implements Factory<HttpClient>, PreDestroy {

    @Inject
    public HttpClientFactoryHK2(Configuration configuration, Provider<DnsLookupService> dnsLookup, Provider<SslContextFactory.Client> clientSslContextFactoryProvider) throws Exception {
        super(configuration, dnsLookup::get, clientSslContextFactoryProvider::get);
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
    }

    @Override
    @Singleton
    public HttpClient provide() {
        return super.provide();
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
