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
package dev.xorcery.reactivestreams.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.api.Session;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SubProtocolHandlerHelpers {
    long CANCEL = Long.MIN_VALUE;
    long COMPLETE = -1L;

    ObjectMapper jsonMapper = new JsonMapper().findAndRegisterModules();

    Session getSession();

    default reactor.util.context.Context parseContext(ObjectNode serverContextNode, Map<String, String> pathParameters, Map<String, List<String>> parameterMap) throws JsonProcessingException {
        Map<String, Object> contextMap = new HashMap<>();
        for (Map.Entry<String, JsonNode> property : serverContextNode.properties()) {
            JsonNode value = property.getValue();
            switch (value.getNodeType()) {
                case STRING -> contextMap.put(property.getKey(), value.asText());
                case BOOLEAN -> contextMap.put(property.getKey(), value.asBoolean());
                case NUMBER -> {
                    if (value.isIntegralNumber()) {
                        contextMap.put(property.getKey(), value.asLong());
                    } else {
                        contextMap.put(property.getKey(), value.asDouble());
                    }
                }
                case OBJECT -> contextMap.put(property.getKey(), jsonMapper.treeToValue(value, Map.class));
                case ARRAY -> contextMap.put(property.getKey(), jsonMapper.treeToValue(value, List.class));
            }
        }
        contextMap.putAll(pathParameters);
        parameterMap.forEach((k, v) -> contextMap.put(k, v.get(0)));

        contextMap.put("request", getSession().getUpgradeRequest());
        contextMap.put("response", getSession().getUpgradeResponse());
        return reactor.util.context.Context.of(contextMap);
    }

    default ObjectNode createServerContext(ContextView contextView) {
        ObjectNode serverContext = JsonNodeFactory.instance.objectNode();
        contextView.forEach((k, v) ->
        {
            if (v instanceof String str) {
                serverContext.set(k.toString(), serverContext.textNode(str));
            } else if (v instanceof Long nr) {
                serverContext.set(k.toString(), serverContext.numberNode(nr));
            } else if (v instanceof Double nr) {
                serverContext.set(k.toString(), serverContext.numberNode(nr));
            } else if (v instanceof Boolean bool) {
                serverContext.set(k.toString(), serverContext.booleanNode(bool));
            }
        });
        return serverContext;
    }
}
