package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.configuration.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public record Neo4jConfiguration(Configuration configuration)
{
    Path databasePath()
    {
        return new File(configuration.getString("home").orElseThrow()).toPath();
    }

    Map<String, String> settings()
    {
        return configuration().getConfiguration("settings").asMap()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        entry.getValue().textValue()
                ));
    }
}
