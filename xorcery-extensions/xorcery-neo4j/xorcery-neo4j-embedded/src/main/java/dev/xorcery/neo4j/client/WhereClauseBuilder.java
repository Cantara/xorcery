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
package dev.xorcery.neo4j.client;

import dev.xorcery.lang.Enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class WhereClauseBuilder {

    public static WhereClauses where() {
        return new WhereClauses();
    }

    public static void appendWhereClauses(StringBuilder query, List<String> whereClauses) {
        if (!whereClauses.isEmpty()) {
            query.append(" WHERE ");
            for (int i = 0; i < whereClauses.size(); i++) {
                String whereClause = whereClauses.get(i);
                if (i > 0) {
                    query.append(" AND ");
                }

                query.append(whereClause);
            }
        }
    }

    /**
     * This builder creates a function that makes it easier to add WHERE clauses to Cypher queries.
     * Specify the parameter type, the expected value type, and what WHERE clause that should be added if such a
     * parameter is encountered
     */
    public static class WhereClauses
            implements BiConsumer<GraphQuery, StringBuilder> {
        Map<Enum<?>, Function<Object, String>> whereClauses = new HashMap<>();

        private WhereClauses() {
        }

        public WhereClauses parameter(Enum<?> parameter, Class<?> valueType, String whereClause) {
            Function<Object, String> current = whereClauses.computeIfAbsent(parameter, p -> value ->
            {
                throw new IllegalArgumentException(
                        "WHERE clause parameter for " + Enums.toField(parameter) + " is of unknown type " + value + "(" +
                                value.getClass().getName() + ")");
            });

            Function<Object, String> converter = value ->
            {
                if (valueType.isInstance(value)) {
                    return whereClause;
                } else {
                    return current.apply(value);
                }
            };

            whereClauses.put(parameter, converter);

            return this;
        }

        @Override
        public void accept(GraphQuery query, StringBuilder string) {

            Map<Enum<?>, Object> parameters = query.getParameters();

            if (!parameters.isEmpty()) {
                // Add query parameters
                List<String> clauses = new ArrayList<String>();
                for (Map.Entry<Enum<?>, Object> entry : parameters.entrySet()) {
                    if (entry.getValue() != null) {
                        Function<Object, String> converter = whereClauses.get(entry.getKey());
                        if (converter != null) {
                            String whereClause = converter.apply(entry.getValue());
                            if (whereClause != null) {
                                clauses.add(whereClause);
                            }
                        }
                    }
                }

                if (!clauses.isEmpty()) {
                    appendWhereClauses(string, clauses);
                }
            }
        }
    }
}
