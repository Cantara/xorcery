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
package dev.xorcery.neo4j.jsonapi.resources;

import dev.xorcery.jaxrs.server.resources.ContextResource;
import dev.xorcery.lang.Enums;
import dev.xorcery.neo4j.client.GraphQuery;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FilteringResource
        extends ContextResource {
    // Filters
    default Filters filters() {
        return new Filters(getUriInfo().getQueryParameters());
    }

    default Filters filter(String name, Enum<?> field) {
        return filters().filter(name, field);
    }

    default Filters filter(Enum<?> field) {
        return filters()
                .filter("filter[" + Enums.toField(field) + "]", field);
    }

    default Filters filter(Enum<?> field, Function<String, Object> valueMapper) {
        return filters()
                .filter("filter[" + Enums.toField(field) + "]", field, valueMapper);
    }

    default GraphQuery applyFilters(Map<String, Enum<?>> filters, GraphQuery data) {
        filters.forEach((k, v) ->
        {
            String value = getUriInfo().getQueryParameters().getFirst(k);
            if (!StringUtils.isBlank(value)) {
                data.parameter(v, value);
            }
        });
        return data;
    }

    default Consumer<GraphQuery> filters(Map<String, Enum<?>> filters) {
        return query -> applyFilters(filters, query);
    }

    interface FilterApplier
            extends BiConsumer<String, GraphQuery> {
    }

    class FilterParameterApplier
            implements FilterApplier {
        private final Enum<?> fieldName;
        private Function<String, Object> vm;

        public FilterParameterApplier(Enum<?> fieldName, Function<String, Object> vm) {
            this.fieldName = fieldName;
            this.vm = vm;
        }

        @Override
        public void accept(String value, GraphQuery graphQuery) {
            Object parsedValue = vm.apply(value);
            graphQuery.parameter(fieldName, parsedValue);
        }
    }

    class FilterParameterReplaceApplier
            implements FilterApplier {
        private final Enum<?> fieldName;
        private final Enum<?> replaces;
        private Function<String, Object> vm;

        public FilterParameterReplaceApplier(Enum<?> fieldName, Enum<?> replaces, Function<String, Object> vm) {
            this.fieldName = fieldName;
            this.replaces = replaces;
            this.vm = vm;
        }

        @Override
        public void accept(String value, GraphQuery graphQuery) {
            Object parsedValue = vm.apply(value);
            graphQuery.parameter(fieldName, parsedValue);
            graphQuery.getParameters().remove(replaces);
        }
    }

    class Filters
            implements Consumer<GraphQuery> {
        private final MultivaluedMap<String, String> queryParameters;
        private final Map<String, FilterApplier> filters = new HashMap<>();

        public Filters(MultivaluedMap<String, String> queryParameters) {
            this.queryParameters = queryParameters;
        }

        public Filters filter(String name, Enum<?> field, Function<String, Object> vm) {
            filters.put(name, new FilterParameterApplier(field, vm));
            return this;
        }

        public Filters filter(String name, Enum<?> field, Enum<?> replacesField, Function<String, Object> vm) {
            filters.put(name, new FilterParameterReplaceApplier(field, replacesField, vm));
            return this;
        }

        public Filters filter(String name, Enum<?> field) {
            return filter(name, field, s -> s);
        }

        public Filters filter(Enum<?> field) {
            return filter("filter[" + Enums.toField(field) + "]", field);
        }

        public Filters filter(Enum<?> field, Function<String, Object> vm) {
            return filter("filter[" + Enums.toField(field) + "]", field, vm);
        }

        @Override
        public void accept(GraphQuery graphQuery) {
            filters.forEach((k, v) ->
            {
                String value = queryParameters.getFirst(k);
                if (!StringUtils.isBlank(value)) {
                    v.accept(value, graphQuery);
                }
            });
        }
    }
}
