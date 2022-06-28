package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author rickardoberg
 */
public record Errors(ArrayNode json)
        implements JsonElement, Iterable<Error> {

    public record Builder(ArrayNode builder)
            implements With<Builder>
    {
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
    }

    public boolean hasErrors() {
        return !array().isEmpty();
    }

    public List<Error> getErrors() {
        return JsonElement.getValuesAs(array(),Error::new);
    }

    @NotNull
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
}
