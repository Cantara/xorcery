package dev.xorcery.opensearch.client;

public enum OpenSearchContext {
    index, // Index name to insert into
    alias, // Index or alias to read from
    host // OpenSearch host URL
}
