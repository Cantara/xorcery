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
package dev.xorcery.thymeleaf.jsonapi.providers;

import dev.xorcery.jsonapi.Link;
import org.glassfish.jersey.uri.UriTemplate;

import java.io.IOException;
import java.util.List;

public class JsonApiHelper {

    public boolean isTemplate(Link link) {
        return !new UriTemplate(link.getHref()).getTemplateVariables().isEmpty();
    }

    public List<String> parameters(Link link) throws IOException {
        if (link == null)
            return null;
        else
            return new UriTemplate(link.getHref()).getTemplateVariables();
    }
}
