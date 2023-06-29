package com.exoreaction.xorcery.configuration.spi;

import com.fasterxml.jackson.databind.JsonNode;

public interface ConfigurationProvider
{
    String getNamespace();

    JsonNode getJson(String name);
}
