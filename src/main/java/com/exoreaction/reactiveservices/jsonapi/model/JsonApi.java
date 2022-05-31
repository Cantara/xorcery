package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record JsonApi(ObjectNode json)
    implements JsonElement
{
}
