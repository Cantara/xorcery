package com.exoreaction.reactiveservices.service.neo4j.client;

import com.exoreaction.util.With;
import org.neo4j.graphdb.Result;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class GraphQuery
        implements With<GraphQuery> {
    public enum Order {
        ASCENDING,
        DESCENDING
    }

    private String name = "default"; // Logical name of the query. Used for text lookups
    private int skip = -1;
    private int limit = -1;
    private Map<Enum<?>, Order> sortOrder = null;
    private Map<Enum<?>, Object> parameters = new HashMap<>();
    private Set<Enum<?>> result = new LinkedHashSet<>();
    private boolean distinct = false;
    private boolean count = false; // Only do a count?
    private int timeout = 0; // No timeout is the default

    private String baseQuery;
    private BiConsumer<GraphQuery,StringBuilder> extend;
    private Function<Enum<?>, String> fieldMapping;
    private final Function<GraphQuery, CompletionStage<GraphResult>> applyQuery;

    public GraphQuery(String baseQuery,
                      Function<Enum<?>,String> fieldMapping,
                      Function<GraphQuery, CompletionStage<GraphResult>> applyQuery) {
        this.baseQuery = baseQuery;
        this.fieldMapping = fieldMapping;
        this.applyQuery = applyQuery;
    }

    public GraphQuery name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Provide a function that is invoked on {@link #build()} to expand the base query.
     * <p>
     * Useful in cases where a static base query is not enough.
     *
     * @param extend
     * @return
     */
    public GraphQuery extend( BiConsumer<GraphQuery,StringBuilder> extend )
    {
        if ( this.extend == null )
        {
            this.extend = extend;
        }
        else
        {
            this.extend = this.extend.andThen( extend );
        }

        return this;
    }


    public GraphQuery timeout(int timeoutInSeconds) {
        this.timeout = timeoutInSeconds;
        return this;
    }

    public GraphQuery parameter(Enum<?> name, Object value) {
        this.parameters.put(name, value);
        return this;
    }

    public GraphQuery skip(int nr) {
        skip = nr;
        return this;
    }

    public GraphQuery limit(int nr) {
        limit = nr;
        return this;
    }

    public GraphQuery clearSort() {
        this.sortOrder = null;
        return this;
    }

    public GraphQuery sort(Enum<?> fieldName, Order order) {
        if (this.sortOrder == null) {
            this.sortOrder = new LinkedHashMap<>();
        }
        this.sortOrder.put(fieldName, order);

        result.add(fieldName);

        return this;
    }

    public GraphQuery result(Enum<?>... fieldNames) {
        return result(Arrays.asList(fieldNames));
    }

    public GraphQuery result(List<Enum<?>> fieldNames) {
        result.addAll(fieldNames);
        return this;
    }

    public Map<Enum<?>, Object> getParameters() {
        return parameters;
    }
    public int getTimeout() {
        return timeout;
    }

    public <T> CompletionStage<Stream<T>> stream(Function<Result.ResultRow, T> mapper) {
        return applyQuery.apply(this).thenApply(gr ->
        {
            List<T> results = new ArrayList<>();

            try {
                try (gr) {
                    gr.getResult().accept((Result.ResultVisitor<Exception>) row -> {
                        results.add(mapper.apply(row));
                        return true;
                    });
                    return results.stream();
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Build the final Cypher query.
     * <p>
     * Starts with the base query string, appends whatever the optional extend function wants, then creates a
     * RETURN clause using the provided function to map fields to strings, and finally adds ORDER BY, SKIP, and LIMIT
     * clauses if necessary.
     *
     * @return final Cypher query
     */
    public String build(  )
    {
        // Base query
        StringBuilder cypher = new StringBuilder( baseQuery );

        // Optionally extend base query
        if ( extend != null )
        {
            extend.accept( this, cypher );
        }

        // Return clause
        if ( !result.isEmpty() )
        {
            cypher.append( " RETURN " );

            if ( distinct )
            {
                cypher.append( "DISTINCT " );
            }

            if ( count )
            {
                cypher.append( "count(*) as total" );
            }
            else
            {
                cypher.append( result.stream().map( fieldMapping ).reduce( ( s1, s2 ) -> s1 + "," + s2 ).orElse( "" ) );
            }
        }

        if ( !count )
        {
            // Sort
            if ( sortOrder != null )
            {
                cypher.append( " ORDER BY " );

                Iterator<Map.Entry<Enum<?>,Order>> itr = sortOrder.entrySet().iterator();
                while ( itr.hasNext() )
                {
                    Map.Entry<Enum<?>,Order> entry = itr.next();
                    cypher.append( Cypher.toField( entry.getKey() ) );
                    if ( entry.getValue() == Order.DESCENDING )
                    {
                        cypher.append( " DESC " );
                    }

                    if ( itr.hasNext() )
                    {
                        cypher.append( ',' );
                    }
                }
            }

            // Skip + Limit
            if ( skip != -1 )
            {
                cypher.append( " SKIP " ).append( skip );
            }
            if ( limit != -1 )
            {
                cypher.append( " LIMIT " ).append( limit );
            }
        }

        return cypher.toString();
    }
}
