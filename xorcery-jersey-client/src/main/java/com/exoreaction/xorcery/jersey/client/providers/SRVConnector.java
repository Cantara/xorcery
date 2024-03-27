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
package com.exoreaction.xorcery.jersey.client.providers;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class SRVConnector
        implements Connector {
    private DnsLookup dnsLookup;
    private final Connector delegate;

    public SRVConnector(DnsLookup dnsLookup, Connector delegate) {
        this.dnsLookup = dnsLookup;
        this.delegate = delegate;
    }

    @Override
    public ClientResponse apply(ClientRequest request) {

        if (request.getUri().getScheme().equals("srv")) {
            URI srvUri = request.getUri();
            List<URI> servers = dnsLookup.resolve(srvUri).join();
            ProcessingException throwable = null;
            for (URI server : servers) {

                if (server.getScheme().equals("srv"))
                    throw new ProcessingException("Could not resolve " + srvUri);

                request.setUri(server);
                ClientResponse clientResponse = null;
                try {
                    return delegate.apply(request);
                } catch (ProcessingException t) {
                    // Try next server
                    throwable = t;
                }
            }
            if (throwable != null)
                throw throwable;

            throw new ProcessingException("Could not resolve " + srvUri);
        }

        return delegate.apply(request);
    }

    @Override
    public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {

        if (request.getUri().getScheme().equals("srv")) {
            URI srvUri = request.getUri();
            return dnsLookup.resolve(srvUri).whenComplete((servers, exception) ->
            {
                if (servers != null) {
                    Iterator<URI> serverIterator = servers.iterator();
                    if (serverIterator.hasNext()) {
                        URI server = serverIterator.next();
                        request.setUri(server);
                        delegate.apply(request, new AsyncConnectorCallback() {
                            @Override
                            public void response(ClientResponse response) {
                                callback.response(response);
                            }

                            @Override
                            public void failure(Throwable failure) {
                                if (serverIterator.hasNext()) {
                                    URI server = serverIterator.next();
                                    request.setUri(server);
                                    delegate.apply(request, this);
                                } else {
                                    callback.failure(failure);
                                }
                            }
                        });
                        callback.response(delegate.apply(request));
                    } else
                        callback.failure(new ProcessingException("Could not resolve " + srvUri));
                } else {
                    callback.failure(exception);
                }
            });
        } else
        {
            return delegate.apply(request, callback);
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void close() {
        delegate.close();
        // We actually never want to shut down the delegated Jetty connector, because it doesn't really own the HttpClient instance
    }
}
