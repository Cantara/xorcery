package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author rickardoberg
 */
public final class Errors
        implements JsonElement, Iterable<Error> {
    private final ArrayNode json;

    /**
     *
     */
    public Errors(ArrayNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ArrayNode builder;

        public Builder(ArrayNode builder) {
            this.builder = builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder error(Error error) {
            builder.add(error.json());
            return this;
        }

        public Errors build() {
            return new Errors(builder);
        }

        public ArrayNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
        }

    }

    public boolean hasErrors() {
        return !array().isEmpty();
    }

    public List<Error> getErrors() {
        return JsonElement.getValuesAs(array(), Error::new);
    }

    @Override
    public Iterator<Error> iterator() {
        return getErrors().iterator();
    }

    public Map<String, Error> getErrorMap() {
        Map<String, Error> map = new HashMap<>();
        for (Error error : getErrors()) {
            String pointer = error.getSource().getPointer();
            if (pointer != null) {
                pointer = pointer.substring(pointer.lastIndexOf('/') + 1);
            }
            map.put(pointer, error);
        }
        return map;
    }

    public String getError() {
        for (Error error : getErrors()) {
            if (error.getSource().getPointer() == null)
                return error.getTitle();
        }

        return null;
    }

    @Override
    public ArrayNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Errors) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Errors[" +
               "json=" + json + ']';
    }

}
