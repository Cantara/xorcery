package com.exoreaction.reactiveservices.service.registry.client;

import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import jakarta.json.Json;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public class RegistryClient
{
    private final WebSocketClient webSocketClient;
    private final HttpClient httpClient;
    private final URI registryUri;

    public RegistryClient( WebSocketClient webSocketClient, URI registryUri )
    {
        this.webSocketClient = webSocketClient;
        httpClient = webSocketClient.getHttpClient();
        this.registryUri = registryUri;
    }

    public void addRegistryListener( RegistryListener listener )
    {
        // Get current registry snapshot
        try
        {
            String registryJson = httpClient.GET( registryUri ).getContentAsString();

            ResourceDocument registry =
                new ResourceDocument( Json.createReader( new StringReader( registryJson ) ).read() );

            // Register for updates
            Link websocket = registry.getLinks().getRel( "events" ).orElseThrow();
            webSocketClient.connect( new ClientEndpoint( listener ), URI.create( websocket.getHref() ) ).get();

            Link link = registry.getLinks().getRel( "servers" ).orElseThrow();

            String servicesJson = httpClient.GET( link.getHref() ).getContentAsString();

            ResourceDocument snapshot =
                new ResourceDocument( Json.createReader( new StringReader( servicesJson ) ).read() );
            listener.snapshot( snapshot );
        }
        catch ( InterruptedException | TimeoutException | ExecutionException | IOException e )
        {
            e.printStackTrace();
        }
    }

    public void addServer( ResourceDocument server ) throws ExecutionException, InterruptedException, TimeoutException
    {
        httpClient.POST( UriBuilder.fromUri( registryUri ).segment( "servers" ).build(  ) )
                  .body( new StringRequestContent( ResourceDocument.APPLICATION_JSON_API, server.toString() ) ).send();
    }

    public static class ClientEndpoint
        implements WebSocketListener
    {
        private final RegistryListener listener;

        public ClientEndpoint( RegistryListener listener )
        {
            this.listener = listener;
        }

        @Override
        public void onWebSocketText( String body )
        {
            ResourceDocument added = new ResourceDocument( Json.createReader( new StringReader( body ) ).read() );
            listener.added( added );
        }
    }
}
