package com.exoreaction.xorcery.jsonschema.jaxrs;

import jakarta.ws.rs.core.MediaType;

public final class MediaTypes
{
    public final static String APPLICATION_JSON_API = "application/vnd.api+json";
    public final static String APPLICATION_JSON_LOGEVENT = "application/vnd.logevent+json";
    public final static String APPLICATION_YAML = "application/yaml";
    public static final String APPLICATION_JSON_SCHEMA = "application/schema+json";

    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JSON_API} media type.
     */
    public final static MediaType APPLICATION_JSON_API_TYPE =
            new MediaType("application", "vnd.api+json");

    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_YAML} media type.
     */
    public final static MediaType APPLICATION_YAML_TYPE =
            new MediaType("application", "yaml");

    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JSON_API} media type.
     */
    public final static MediaType APPLICATION_JSON_SCHEMA_TYPE =
            new MediaType("application", "schema+json");
}
