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
package com.exoreaction.xorcery.jsonapi.server.resources;

import com.exoreaction.xorcery.jaxrs.server.resources.ContextResource;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static com.exoreaction.xorcery.jsonapi.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

@Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
public interface JsonApiResource
        extends ContextResource {

    @OPTIONS
    default Response options() {
        return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, PATCH, OPTIONS")
                .header("Access-Control-Allow-Headers", "content-type, accept, cookie, authorization")
                .build();
    }
}