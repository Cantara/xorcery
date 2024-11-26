package dev.xorcery.jetty.server;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record GzipConfiguration(Configuration configuration) {

    public static GzipConfiguration get(Configuration configuration) {
        return new GzipConfiguration(configuration.getConfiguration("jetty.server.gzip"));
    }

    public List<String> getExcludedMediaTypes()
    {
        return configuration.getListAs("excluded.mediatypes", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getExcludedMethods()
    {
        return configuration.getListAs("excluded.methods", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getExcludedPaths()
    {
        return configuration.getListAs("excluded.paths", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getIncludedMediaTypes()
    {
        return configuration.getListAs("included.mediatypes", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getIncludedMethods()
    {
        return configuration.getListAs("included.methods", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getIncludedPaths()
    {
        return configuration.getListAs("included.paths", JsonNode::asText).orElse(Collections.emptyList());
    }

    public Optional<Integer> getMinGzipSize() {
        return configuration.getInteger("minGzipSize");
    }

    public Optional<Boolean> isSyncFlush() {
        return configuration.getBoolean("syncFlush");
    }
}
