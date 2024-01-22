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
package com.exoreaction.xorcery.jersey.server.resources;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerApplication
        extends ResourceConfig {
    public ServerApplication(Configuration configuration) {
        property(ServerProperties.WADL_FEATURE_DISABLE, Boolean.TRUE);
        property(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE);

        List<String> mediaTypesList = new ArrayList<>();
        configuration.getObjectAs("jersey.server.mediaTypes", JsonElement::asMap).ifPresent(mappings ->
        {
            mappings.forEach((suffix, mediaType)-> mediaTypesList.add(suffix+":"+mediaType.textValue()));
        });

        String mediaTypesString = String.join(",", mediaTypesList);
        property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypesString);

        LogManager.getLogger(getClass()).debug("Media types:\n" + mediaTypesString.replace(',', '\n'));
    }
}
