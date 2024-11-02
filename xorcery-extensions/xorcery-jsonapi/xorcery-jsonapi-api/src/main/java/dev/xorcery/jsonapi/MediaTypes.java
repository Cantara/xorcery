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
package dev.xorcery.jsonapi;

public class MediaTypes
{
    public final static String APPLICATION_JSON_API = "application/vnd.api+json";
    public final static String APPLICATION_YAML = "application/yaml";
    public static final String APPLICATION_JSON_SCHEMA = "application/schema+json";

    // For resources that produce JSON-API ResourceDocument that can be also be rendered as HTML or YAML
    public static final String PRODUCES_JSON_API_TEXT_HTML_YAML = "text/html"+";qs=1,"+APPLICATION_JSON_API+";qs=0.5,"+"application/json"+";qs=0.4,"+APPLICATION_YAML+";qs=0.3";

    // For resources that produce JSON that can be also be rendered as HTML or YAML
    public static final String PRODUCES_JSON_TEXT_HTML_YAML = "text/html"+";qs=1,"+"application/json"+";qs=0.5,"+APPLICATION_YAML+";qs=0.5";
}
