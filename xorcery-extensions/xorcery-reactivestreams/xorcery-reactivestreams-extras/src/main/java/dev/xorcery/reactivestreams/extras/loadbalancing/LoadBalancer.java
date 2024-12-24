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
package dev.xorcery.reactivestreams.extras.loadbalancing;

import dev.xorcery.dns.client.providers.DnsLookupService;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Service
public class LoadBalancer {

    // Load balancing statistics names to use for picking server to select
    public static final String CONNECTIONS = "connections";

    public static Comparator<Map<String, Integer>> comparator(String... statsToCompare)
    {
        return (o1, o2) -> {
            for (String stat : statsToCompare) {
                int v1 = o1.get(stat);
                int v2 = o2.get(stat);
                if (v1 != v2)
                {
                    return v1 - v2;
                }
            }
            return 0;
        };
    }

    private final DnsLookupService dnsLookupService;
    private final HttpClient httpClient;
    private final Logger logger;

    @Inject
    public LoadBalancer(DnsLookupService dnsLookupService, HttpClient httpClient, Logger logger) {
        this.dnsLookupService = dnsLookupService;
        this.httpClient = httpClient;
        this.logger = logger;
    }

    public <T> BiFunction<Flux<T>, ContextView, Publisher<T>> loadBalance(Comparator<Map<String, Integer>> loadBalancingComparator) {

        return (flux, contextView) ->
        {
            URI serverUri = new ContextViewElement(contextView).getURI(ClientWebSocketStreamContext.serverUri).orElseThrow();

            List<URI> servers = dnsLookupService.resolve(serverUri).orTimeout(10, TimeUnit.SECONDS).join();

            try {
                // Setup all result structures
                List<ServerStats> serverStats = new CopyOnWriteArrayList<>();
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (URI server : servers) {
                    futures.add(new CompletableFuture<>());
                }

                // Send loadbalancer stats request to all servers
                for (int i = 0; i < servers.size(); i++) {
                    URI server = servers.get(i);
                    String newScheme = server.getScheme().equals("wss") ? "https" : "http";
                    URI httpUri = new URI(newScheme, server.getUserInfo(), server.getHost(), server.getPort(), server.getPath(), server.getQuery(), server.getFragment());
                    int finalI = i;
                    httpClient.newRequest(httpUri)
                            .headers(headers -> headers
                                    .add(HttpHeader.SEC_WEBSOCKET_VERSION, Integer.toString(13))
                                    .add(HttpHeader.UPGRADE, "websocket")
                                    .add(HttpHeader.CONNECTION, "upgrade")
                                    .add(HttpHeader.SEC_WEBSOCKET_KEY, "dGhlIHNhbXBsZSBub25jZQ==")
                                    .add("Xorcery-LoadBalancing", "connections"))
                            .send(result -> {
                                if (result.isSucceeded()) {
                                    try {
                                        if (result.getResponse().getStatus() == HttpStatus.NO_CONTENT_204) {
                                            HttpFields headers = result.getResponse().getHeaders();
                                            String loadBalancingResponse = headers.get("Xorcery-LoadBalancing");
                                            if (loadBalancingResponse != null) {
                                                String[] weights = loadBalancingResponse.split(",");
                                                Map<String, Integer> stats = new LinkedHashMap<>();
                                                for (String weight : weights) {
                                                    String[] weightParts = weight.split("=");
                                                    String key = weightParts[0];
                                                    int value = Integer.parseInt(weightParts[1]);
                                                    stats.put(key, value);
                                                }
                                                serverStats.add(new ServerStats(server, stats));
                                                futures.get(finalI).complete(null);
                                            }
                                        }
                                    } catch (Throwable e) {
                                        futures.get(finalI).completeExceptionally(e);
                                    }
                                } else {
                                    futures.get(finalI).completeExceptionally(result.getFailure());
                                }
                            });
                }

                // Wait for all requests to finish
                for (int i = 0; i < futures.size(); i++) {
                    CompletableFuture<Void> future = futures.get(i);
                    try {
                        future.orTimeout(10, TimeUnit.SECONDS).join();
                    } catch (Throwable e) {
                        logger.warn("Could not get loadbalancer information from " + servers.get(i), e);
                    }
                }
                if (serverStats.isEmpty()) {
                    return Flux.error(new IllegalStateException("No valid servers found"));
                }
                serverStats.sort(Comparator.comparing(ServerStats::weights, loadBalancingComparator));
                return flux.contextWrite(Context.of(ClientWebSocketStreamContext.serverUri, serverStats.get(0).uri()));
            } catch (Throwable e) {
                return Flux.error(e);
            }
        };
    }

    record ServerStats(URI uri, Map<String, Integer> weights) {
    }
}
