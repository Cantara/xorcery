package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
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
