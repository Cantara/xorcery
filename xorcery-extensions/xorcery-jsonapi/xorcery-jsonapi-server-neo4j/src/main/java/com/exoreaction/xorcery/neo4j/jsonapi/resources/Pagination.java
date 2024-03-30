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
package com.exoreaction.xorcery.neo4j.jsonapi.resources;

import com.exoreaction.xorcery.jsonapi.JsonApiRels;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.neo4j.client.GraphQuery;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Pagination
        implements Consumer<GraphQuery> {
    private final Supplier<UriBuilder> uriBuilderSupplier;
    private final MultivaluedMap<String, String> queryParameters;
    private final Links.Builder links;
    private final String relationshipPath;
    private Optional<Integer> skip;
    private Optional<Integer> limit;
    private GraphQuery query;
    private String skipParam = encode("page[skip]");
    private String limitParam;

    public Pagination(Supplier<UriBuilder> uriBuilderSupplier, MultivaluedMap<String, String> queryParameters,
                      Links.Builder links) {
        this(uriBuilderSupplier, queryParameters, links, "");
    }

    public Pagination(Supplier<UriBuilder> uriBuilderSupplier, MultivaluedMap<String, String> queryParameters,
                      Links.Builder links, String relationshipPath) {
        this.uriBuilderSupplier = uriBuilderSupplier;
        this.queryParameters = queryParameters;
        this.links = links;
        this.relationshipPath = relationshipPath;
        this.skip = Optional.empty();
        this.limit = Optional.empty();
        this.limitParam = encode("page[limit]");
        if (relationshipPath.length() > 0) {
            this.skipParam += encode("[" + relationshipPath + "]");
            this.limitParam += encode("[" + relationshipPath + "]");
        }
        String skip = queryParameters.getFirst(this.skipParam);
        if (!StringUtils.isBlank(skip)) {
            try {
                this.skip = Optional.of(Integer.parseInt(skip));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        String limit = queryParameters.getFirst(limitParam);
        if (!StringUtils.isBlank(limit)) {
            try {
                this.limit = Optional.of(Math.min(Integer.parseInt(limit), 1000));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }

    @Override
    public void accept(GraphQuery graphQuery) {
        this.query = graphQuery;
        this.skip.ifPresent((s) -> query.skip(s));
        this.limit.ifPresent((lim) -> query.limit(lim));

        this.skip.ifPresent(s ->
        {
            if (s > 0) {
                {
                    // Previous link
                    UriBuilder uriBuilder = uriBuilderSupplier.get();

                    String skipParam = Integer.toString(Math.max(0, s - query.getLimit()));
                    uriBuilder.replaceQueryParam(this.skipParam, skipParam);

                    this.links.link(JsonApiRels.prev, uriBuilder.build().toASCIIString());
                }

                {
                    // First link
                    UriBuilder uriBuilder = uriBuilderSupplier.get();

                    String skipParam = "0";
                    uriBuilder.replaceQueryParam(this.skipParam, skipParam);

                    this.links.link(JsonApiRels.first, uriBuilder.build().toASCIIString());
                }
            }
        });

        graphQuery.onStream(this::skipLimit);
    }

    private Stream<Object> skipLimit(Stream<Object> stream) {
        AtomicInteger count = new AtomicInteger();

        return stream.peek(map ->
        {
            if (count.incrementAndGet() == this.query.getLimit()) {
                UriBuilder uriBuilder = uriBuilderSupplier.get();

                if (!relationshipPath.equals("")) {
                    // Fix include parameter by removing path prefix
                    String include = URLDecoder.decode(queryParameters.getFirst("include"), StandardCharsets.UTF_8);
                    if (include != null) {
                        String[] includes = include.split(",");
                        StringBuilder includeBuilder = new StringBuilder();
                        for (String includePart : includes) {
                            // Only include paths with this path as prefix
                            if (includePart.startsWith(relationshipPath + ".")) {
                                if (includeBuilder.length() > 0) {
                                    includeBuilder.append(',');
                                }

                                includeBuilder.append(includePart.substring(relationshipPath.length() + 1));
                            }
                        }
                        uriBuilder.replaceQueryParam("include", includeBuilder.toString());
                    }

                    // Rewrite skip/limit parameters
                    queryParameters.forEach((key, value) ->
                    {
                        if (key.equals(skipParam)) {
                            // Replace with page[skip]
                            uriBuilder.replaceQueryParam(this.skipParam, (Object[]) null);

                        } else if (key.startsWith(skipParam.substring(0, skipParam.length() - 3))) {
                            // Sub-include, so remove prefix
                            uriBuilder.replaceQueryParam(key, (Object[]) null);
                            String newKey = encode("page[skip][") + key.substring(skipParam.length() - 2);
                            uriBuilder.replaceQueryParam(newKey, value.get(0));
                        } else if (key.equals(limitParam)) {
                            // Replace with page[limit]
                            uriBuilder.replaceQueryParam(this.limitParam, (Object[]) null);
                            uriBuilder.replaceQueryParam(encode("page[limit]"), value.get(0));

                        } else if (key.startsWith(limitParam.substring(0, limitParam.length() - 3))) {
                            // Sub-include, so remove prefix
                            uriBuilder.replaceQueryParam(key, (Object[]) null);
                            String newKey = encode("page[limit][") + key.substring(limitParam.length() - 2);
                            uriBuilder.replaceQueryParam(newKey, value.get(0));
                        } else if (key.startsWith(encode("page[skip][")) ||
                                key.startsWith(encode("page[limit]["))) {
                            // Wrong relationship prefix, remove
                            uriBuilder.replaceQueryParam(key, (Object[]) null);
                        }
                    });

                    uriBuilder.replaceQueryParam(encode("page[skip]"), calcSkip());
                } else {
                    uriBuilder.replaceQueryParam(encode("page[skip]"), calcSkip());
                }

                this.links.link(JsonApiRels.next, uriBuilder.build().toASCIIString());
            }
        });
    }

    private String calcSkip() {
        int skip = this.query.getSkip();
        if (skip < 0) {
            skip = 0;
        }
        return Integer.toString(skip + this.query.getLimit());
    }

    private String encode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }
}
