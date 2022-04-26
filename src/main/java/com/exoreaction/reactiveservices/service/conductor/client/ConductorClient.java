package com.exoreaction.reactiveservices.service.conductor.client;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
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

public class ConductorClient
{
    private final WebSocketClient webSocketClient;
    private final HttpClient httpClient;
    private final URI conductorUri;

    public ConductorClient( WebSocketClient webSocketClient, URI conductorUri )
    {
        this.webSocketClient = webSocketClient;
        httpClient = webSocketClient.getHttpClient();
        this.conductorUri = conductorUri;
    }

    public void addConductorListener( RegistryListener listener )
    {
        // Get current conductor snapshot
        try
        {
            String conductorJson = httpClient.GET( conductorUri ).getContentAsString();

            ResourceDocument conductor =
                new ResourceDocument( Json.createReader( new StringReader( conductorJson ) ).read() );

            // Register for updates
            Link websocket = conductor.getLinks().getRel( "events" ).orElseThrow();
            webSocketClient.connect( new ClientEndpoint( listener ), URI.create( websocket.getHref() ) ).get();

            Link link = conductor.getLinks().getRel( "patterns" ).orElseThrow();

            String patternsJson = httpClient.GET( link.getHref() ).getContentAsString();

            ResourceDocument snapshot =
                new ResourceDocument( Json.createReader( new StringReader( patternsJson ) ).read() );
//            listener.snapshot( snapshot );
        }
        catch ( InterruptedException | TimeoutException | ExecutionException | IOException e )
        {
            e.printStackTrace();
        }
    }

    public void addPatterns( ResourceDocument server ) throws ExecutionException, InterruptedException, TimeoutException
    {
        httpClient.POST( UriBuilder.fromUri( conductorUri ).segment( "patterns" ).build(  ) )
                  .body( new StringRequestContent( MediaTypes.APPLICATION_JSON_API, server.toString() ) ).send();
    }

    public class ClientEndpoint
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
//            listener.added( added );
        }
    }
}
