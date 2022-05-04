package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.JsonObject;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record JsonApi(JsonObject json)
    implements JsonElement
{
}
