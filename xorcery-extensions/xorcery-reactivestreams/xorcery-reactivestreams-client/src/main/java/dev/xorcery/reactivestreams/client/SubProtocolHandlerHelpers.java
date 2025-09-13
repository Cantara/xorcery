package dev.xorcery.reactivestreams.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.spi.MessageReader;
import dev.xorcery.reactivestreams.spi.MessageWorkers;
import dev.xorcery.reactivestreams.spi.MessageWriter;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SubProtocolHandlerHelpers {
    ObjectMapper jsonMapper = new JsonMapper().findAndRegisterModules();

    Session getSession();
    ReactiveStreamSubProtocol getSubProtocol();
    MessageWorkers getMessageWorkers();

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

    default ObjectNode createClientContext(ContextView contextView) {
        ObjectNode clientContext = JsonNodeFactory.instance.objectNode();
        contextView.forEach((k, v) ->
        {
            if (v instanceof String str) {
                clientContext.set(k.toString(), clientContext.textNode(str));
            } else if (v instanceof Long nr) {
                clientContext.set(k.toString(), clientContext.numberNode(nr));
            } else if (v instanceof Double nr) {
                clientContext.set(k.toString(), clientContext.numberNode(nr));
            } else if (v instanceof Boolean bool) {
                clientContext.set(k.toString(), clientContext.booleanNode(bool));
            }
        });
        return clientContext;
    }

    default <T> MessageWriter<T> getWriter(String serverAcceptType, Class<T> outputType) {
        if (serverAcceptType == null) {
            getSession().close(StatusCode.SERVER_ERROR, getSubProtocol()+" subprotocol requires Accept header", Callback.NOOP);
            return null;
        }
        MessageWriter<T> writer = getMessageWorkers().newWriter(outputType, outputType, serverAcceptType);
        if (writer == null) {
            getSession().close(StatusCode.SERVER_ERROR, "cannot handle Accept type:" + serverAcceptType, Callback.NOOP);
            return null;
        }
        return writer;
    }

    default <T> MessageReader<T> getReader(String serverContentType, Class<T> inputType) {
        if (serverContentType == null) {
            getSession().close(StatusCode.SERVER_ERROR, "subscriberWithResult subprotocol requires Content-Type header", Callback.NOOP);
            return null;
        }
        MessageReader<T> reader = getMessageWorkers().newReader(inputType, inputType, serverContentType);
        if (reader == null) {
            getSession().close(StatusCode.SERVER_ERROR, "cannot handle Content-Type:" + serverContentType, Callback.NOOP);
            return null;
        }
        return reader;
    }

}

