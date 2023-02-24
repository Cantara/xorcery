package com.exoreaction.xorcery.service.jersey.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.hosts.HostsFileParser;
import org.xbill.DNS.lookup.LookupSession;

import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class SRVConnector
        implements Connector {
    private DnsLookup dnsLookup;
    private final Connector delegate;
    private String scheme;

    public SRVConnector(DnsLookup dnsLookup, String scheme, Connector delegate) {
        this.dnsLookup = dnsLookup;
        this.delegate = delegate;
        this.scheme = scheme;
    }

    @Override
    public ClientResponse apply(ClientRequest request) {

        if (request.getUri().getScheme().equals("srv")) {
            URI srvUri = request.getUri();
            List<URI> servers = dnsLookup.resolve(srvUri).join();
            ProcessingException throwable = null;
            for (URI server : servers) {
                server = UriBuilder.fromUri(server).scheme(scheme).build();
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
                        URI server = UriBuilder.fromUri(serverIterator.next()).scheme(scheme).build();
                        request.setUri(server);
                        delegate.apply(request, new AsyncConnectorCallback() {
                            @Override
                            public void response(ClientResponse response) {
                                callback.response(response);
                            }

                            @Override
                            public void failure(Throwable failure) {
                                if (serverIterator.hasNext()) {
                                    URI server = UriBuilder.fromUri(serverIterator.next()).scheme(scheme).build();
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
    }
}
