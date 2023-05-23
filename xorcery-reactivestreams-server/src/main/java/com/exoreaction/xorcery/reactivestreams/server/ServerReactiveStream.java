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
package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.api.UpgradeRequest;

import java.util.function.Consumer;

public abstract class ServerReactiveStream {

    protected Consumer<Configuration.Builder> addUpgradeRequestConfiguration(UpgradeRequest upgradeRequest)
    {
        return builder ->
        {
            ObjectNode sessionJson = JsonNodeFactory.instance.objectNode();
            ObjectNode headers = sessionJson.objectNode();
            upgradeRequest.getHeaders().forEach((name, values) ->
            {
                // Headers
                ArrayNode valueArray = sessionJson.arrayNode(values.size());
                for (String value : values) {
                    valueArray.add(value);
                }
                headers.set(name, valueArray);
            });
            sessionJson.set("headers", headers);
            sessionJson.set("host", sessionJson.textNode(upgradeRequest.getHost()));

            builder.add("session", sessionJson);
        };
    }
}
