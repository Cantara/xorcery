package com.exoreaction.reactiveservices.service.neo4j.client;

import com.exoreaction.util.builders.With;
import com.exoreaction.util.function.FallbackFunction;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Result;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class GraphQuery
        implements With<GraphQuery> {

    public enum Order {
        ASCENDING,
        DESCENDING
    }

    private String name = "default"; // Name of the query. Useful for analytics
    private String database = "default"; // Name of the database/schema. Useful for multi tenancy cases.

    private int skip = -1;
    private int limit = -1;
    private Map<Enum<?>, Order> sortOrder = null;
    private Map<Enum<?>, Object> parameters = new HashMap<>();
    private Set<Enum<?>> results = new LinkedHashSet<>();
    private boolean distinct = false;
    private boolean count = false; // Only do a count?
    private int timeout = 0; // No timeout is the default

    private String baseQuery;
    private BiConsumer<GraphQuery, StringBuilder> where;
    private Function<Enum<?>, String> fieldMapping;
    private final Function<GraphQuery, CompletionStage<GraphResult>> applyQuery;
    private Function<Stream<Map<String, Object>>, Stream<Map<String, Object>>> streamOperator = UnaryOperator.identity();

    public GraphQuery(String baseQuery,
                      Function<Enum<?>, String> fieldMapping,
                      Function<GraphQuery, CompletionStage<GraphResult>> applyQuery) {
        this.baseQuery = baseQuery;
        this.fieldMapping = fieldMapping;
        this.applyQuery = applyQuery;
    }

    public GraphQuery name(String name) {
        this.name = name;
        return this;
    }

    public GraphQuery database(String name) {
        this.database = name;
        return this;
    }

    public GraphQuery mappings(Function<Enum<?>, String> customFieldMappings) {
        this.fieldMapping = new FallbackFunction<>(customFieldMappings, fieldMapping);
        return this;
    }

    /**
     * Provide a function that is invoked on {@link #build()} to create the where clauses.
     *
     * @param clauses
     * @return
     */
    public GraphQuery where(BiConsumer<GraphQuery, StringBuilder> clauses) {
        if (this.where == null) {
            this.where = clauses;
        } else {
            this.where = this.where.andThen(clauses);
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

        results.add(fieldName);

        return this;
    }

    public GraphQuery results(Enum<?>... fieldNames) {
        return results(Arrays.asList(fieldNames));
    }

    public GraphQuery results(List<Enum<?>> fieldNames) {
        results.addAll(fieldNames);
        return this;
    }

    @SafeVarargs
    public final GraphQuery onStream(UnaryOperator<Stream<Map<String, Object>>>... streamOperators) {
        for (UnaryOperator<Stream<Map<String, Object>>> operator : streamOperators) {
            this.streamOperator = operator.andThen(this.streamOperator);
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDatabase() {
        return database;
    }

    public int getTimeout() {
        return timeout;
    }

    public Map<Enum<?>, Object> getParameters() {
        return parameters;
    }

    public Set<Enum<?>> getResults() {
        return this.results;
    }

    public boolean hasResultFrom(Class<? extends Enum<?>> enumType) {
        for (Enum<?> anEnum : results) {
            if (enumType.isInstance(anEnum)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasParameterFrom(Class<? extends Enum<?>> enumType) {
        for (Enum<?> anEnum : parameters.keySet()) {
            if (enumType.isInstance(anEnum)) {
                return true;
            }
        }
        return false;
    }

    public int getSkip() {
        return skip;
    }

    public int getLimit() {
        return limit;
    }

    public <T> CompletionStage<GraphResult> execute() {
        return applyQuery.apply(this);
    }

    public <T> CompletionStage<Stream<T>> stream(Function<RowModel, T> mapper) {
        return applyQuery.apply(this).thenApply(gr ->
        {
            List<T> results = new ArrayList<>();

            try {
                try (gr) {
                    gr.getResult().accept((Result.ResultVisitor<Exception>) row -> {
                        results.add(mapper.apply(new RowModel(row)));
                        return true;
                    });
                    return results.stream();
                }
            } catch (CompletionException e) {
                throw e;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public <T> CompletionStage<T> first(Function<RowModel, T> mapper) {
        return applyQuery.apply(this).thenApply(gr ->
        {
            List<T> results = new ArrayList<>();

            try {
                try (gr) {
                    gr.getResult().accept((Result.ResultVisitor<Exception>) row -> {
                        results.add(mapper.apply(new RowModel(row)));
                        return false;
                    });
                    return results.stream().findFirst().orElseThrow(NotFoundException::new);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletionStage<Long> count() {
        count = true;
        return applyQuery.apply(this).thenApply(gr ->
        {
            AtomicLong counter = new AtomicLong();
            try {
                try (gr) {
                    gr.getResult().accept((Result.ResultVisitor<Exception>) row -> {
                        counter.set(row.getNumber("total").longValue());
                        return false;
                    });
                    return counter.get();
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Build the final Cypher query.
     * <p>
     * Starts with the base query string, appends whatever the optional where function wants, then creates a
     * RETURN clause using the provided function to map fields to strings, and finally adds ORDER BY, SKIP, and LIMIT
     * clauses if necessary.
     *
     * @return final Cypher query
     */
    public String build() {
        // Base query
        StringBuilder cypher = new StringBuilder(baseQuery);

        // Optionally add where clauses
        if (where != null) {
            where.accept(this, cypher);
        }

        // Return clause
        if (!results.isEmpty()) {
            cypher.append(" RETURN ");

            if (distinct) {
                cypher.append("DISTINCT ");
            }

            if (count) {
                cypher.append("count(*) as total");
            } else {
                cypher.append(results.stream().map(fieldMapping).reduce((s1, s2) -> s1 + "," + s2).orElse(""));
            }
        }

        if (!count) {
            // Sort
            if (sortOrder != null) {
                cypher.append(" ORDER BY ");

                Iterator<Map.Entry<Enum<?>, Order>> itr = sortOrder.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry<Enum<?>, Order> entry = itr.next();
                    cypher.append(Cypher.toField(entry.getKey()));
                    if (entry.getValue() == Order.DESCENDING) {
                        cypher.append(" DESC ");
                    }

                    if (itr.hasNext()) {
                        cypher.append(',');
                    }
                }
            }

            // Skip + Limit
            if (skip != -1) {
                cypher.append(" SKIP ").append(skip);
            }
            if (limit != -1) {
                cypher.append(" LIMIT ").append(limit);
            }
        }

        return cypher.toString();
    }
}
