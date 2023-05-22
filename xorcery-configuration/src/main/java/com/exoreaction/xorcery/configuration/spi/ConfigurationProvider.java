package com.exoreaction.xorcery.configuration.spi;

import com.fasterxml.jackson.databind.JsonNode;

public interface ConfigurationProvider
{
    JsonNode getJson(String name);
}
