package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */
public record JsonApi(ObjectNode json)
        implements JsonElement {
}
