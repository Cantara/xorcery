package com.exoreaction.xorcery.service.jersey.server.resources;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerApplication
        extends ResourceConfig {
    public ServerApplication(Configuration configuration) throws IOException {
        property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
        property(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE);

        List<String> mediaTypesList = new ArrayList<>();
        configuration.getObjectAs("jersey.server.mediaTypes", JsonElement::asMap).ifPresent(mappings ->
        {
            mappings.forEach((suffix, mediaType)-> mediaTypesList.add(suffix+":"+mediaType.textValue()));
        });

        String mediaTypesString = String.join(",", mediaTypesList);
        property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypesString);

        LogManager.getLogger(getClass()).info("Media types:\n" + mediaTypesString.replace(',', '\n'));
    }
}
