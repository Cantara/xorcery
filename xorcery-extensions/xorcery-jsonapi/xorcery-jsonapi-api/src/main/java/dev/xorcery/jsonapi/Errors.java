/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.jsonapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author rickardoberg
 */
public record Errors(ArrayNode json)
        implements JsonElement, Iterable<Error> {

    public static Builder newErrors(){
        return new Builder();
    }

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
