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
package com.exoreaction.xorcery.service.reactivestreams.client;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.common.LocalStreamFactories;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.ChangeableRunLevelFuture;
import org.glassfish.hk2.runlevel.ProgressStartedListener;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Note: this is set to run level 0 primarily so that client streams are the last to close when an application shuts down.
 */
@Service(name = "reactivestreams.client")
@ContractsProvided({ReactiveStreamsClient.class, PreDestroy.class})
@RunLevel(0)
public class ReactiveStreamsClientServiceHK2 extends com.exoreaction.xorcery.service.reactivestreams.client.ReactiveStreamsClientService
        implements PreDestroy {

    @Inject
    public ReactiveStreamsClientServiceHK2(Configuration configuration,
                                           MessageWorkers messageWorkers,
                                           HttpClient httpClient,
                                           DnsLookupService dnsLookup,
                                           MetricRegistry metricRegistry,
                                           Provider<LocalStreamFactories> localStreamFactoriesProvider) throws Exception {
        super(configuration, messageWorkers, httpClient, dnsLookup, metricRegistry, localStreamFactoriesProvider::get);
    }
}
