package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * @author rickardoberg
 */
public final class JsonApi
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public JsonApi(ObjectNode json) {
        this.json = json;
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (JsonApi) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "JsonApi[" +
               "json=" + json + ']';
    }

}
