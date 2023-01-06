package com.exoreaction.xorcery.service.jersey.server.resources;

import com.exoreaction.xorcery.util.Resources;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServerApplication
        extends ResourceConfig {
    public ServerApplication() throws IOException {
        property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
        property(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE);

        Iterable<URL> mediaTypes = Resources.getResources("META-INF/mediatypes.conf");
        List<String> mediaTypesList = new ArrayList<>();
        for (URL mediaTypesUrl : mediaTypes) {
            try (InputStream mediaTypesStream = mediaTypesUrl.openStream()) {
                if (mediaTypesStream != null) {
                    String mediaTypesFile = new String(mediaTypesStream.readAllBytes(), StandardCharsets.UTF_8);
                    mediaTypesFile = mediaTypesFile.replaceAll("\r?\n", ",");
                    mediaTypesList.add(mediaTypesFile);
                }
            }
        }

        String mediaTypesString = String.join(",", mediaTypesList);
        property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypesString);

        LogManager.getLogger(getClass()).info("Media types:\n" + mediaTypesString.replace(',', '\n'));
    }
}
