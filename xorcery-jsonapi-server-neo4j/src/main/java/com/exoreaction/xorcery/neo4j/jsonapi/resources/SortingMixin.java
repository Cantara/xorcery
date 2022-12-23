package com.exoreaction.xorcery.neo4j.jsonapi.resources;

import com.exoreaction.xorcery.jsonapi.server.resources.ResourceContext;
import com.exoreaction.xorcery.service.neo4j.client.GraphQuery;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

public interface SortingMixin
        extends ResourceContext {
    default <T extends Enum<T>> Consumer<GraphQuery> sort(Class<T> enumType) {
        return query ->
        {
            String sort = getFirstQueryParameter("sort");
            if (!StringUtils.isBlank(sort)) {
                try {
                    GraphQuery.Order order = GraphQuery.Order.ASCENDING;
                    if (sort.startsWith("-")) {
                        order = GraphQuery.Order.DESCENDING;
                        sort = sort.substring(1);
                    }
                    int underscoreIndex = sort.indexOf("_");
                    if (underscoreIndex > -1) {
                        sort = sort.substring(underscoreIndex + 1);
                    }
                    T fieldName = Enum.valueOf(enumType, sort);
                    query.clearSort();
                    query.sort(fieldName, order);
                } catch (IllegalArgumentException e) {
                    // Ignore, this is a sort for something else
                }
            }
        };
    }
}
