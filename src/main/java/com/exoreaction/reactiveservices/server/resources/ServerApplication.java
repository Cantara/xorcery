package com.exoreaction.reactiveservices.server.resources;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerApplication
    extends ResourceConfig
{
    public ServerApplication() throws IOException {
        property(ServerProperties.WADL_FEATURE_DISABLE, "true");

        String mediaTypes = Files.readString(Path.of(URI.create(getClass().getResource("/mediatypes.conf").toString())), StandardCharsets.UTF_8)
                .replace('\n',',');
        property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypes);

        packages("com.exoreaction.reactiveservices.jaxrs");
        packages("com.exoreaction.reactiveservices.server.resources");

    }
}
