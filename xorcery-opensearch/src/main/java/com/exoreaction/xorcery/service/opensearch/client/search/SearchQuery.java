package com.exoreaction.xorcery.service.opensearch.client.search;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SearchQuery {

    public static ObjectNode match(String textEntry) {
        return JsonNodeFactory.instance.objectNode().set("match",
                JsonNodeFactory.instance.objectNode().set("text_entry", JsonNodeFactory.instance.textNode(textEntry)));
    }

    public static ObjectNode match_all() {
        return JsonNodeFactory.instance.objectNode().set("match_all", JsonNodeFactory.instance.objectNode());
    }
}
