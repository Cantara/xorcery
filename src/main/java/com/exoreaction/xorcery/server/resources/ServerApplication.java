package com.exoreaction.xorcery.server.resources;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerApplication
    extends ResourceConfig
{
    public ServerApplication() throws IOException {
        property(ServerProperties.WADL_FEATURE_DISABLE, "true");

        try (InputStream mediaTypesStream = getClass().getResourceAsStream("/mediatypes.conf"))
        {
            if (mediaTypesStream != null)
            {
                String mediaTypes = new String(mediaTypesStream.readAllBytes(), StandardCharsets.UTF_8).replace('\n',',');
                property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypes);
            }
        }
    }
}
