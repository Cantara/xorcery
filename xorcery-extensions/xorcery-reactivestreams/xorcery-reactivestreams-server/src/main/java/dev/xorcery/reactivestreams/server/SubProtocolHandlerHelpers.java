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

    default reactor.util.context.Context parseContext(ObjectNode serverContextNode) throws JsonProcessingException {
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
        contextMap.put("request", getSession().getUpgradeRequest());
        contextMap.put("response", getSession().getUpgradeResponse());
        return reactor.util.context.Context.of(contextMap);
    }

    default ObjectNode createServerContext(ContextView contextView, Map<String, String> pathParameters, Map<String, List<String>> parameterMap) {
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

        pathParameters.forEach((k, v) -> serverContext.set(k, JsonNodeFactory.instance.textNode(v)));
        parameterMap.forEach((k, v) -> serverContext.set(k, JsonNodeFactory.instance.textNode(v.get(0))));
        return serverContext;
    }
}
