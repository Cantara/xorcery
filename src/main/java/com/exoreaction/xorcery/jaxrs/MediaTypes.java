package com.exoreaction.xorcery.jaxrs;

import jakarta.ws.rs.core.MediaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class MediaTypes
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

    // For resources that produce JSON-API ResourceDocument that can be also be rendered as HTML or YAML
    public static final String PRODUCES_JSON_API_TEXT_HTML_YAML = MediaType.TEXT_HTML+";qs=1,"+APPLICATION_JSON_API+";qs=0.5,"+APPLICATION_JSON+";qs=0.4,"+APPLICATION_YAML+";qs=0.3";

    // For resources that produce JSON that can be also be rendered as HTML or YAML
    public static final String PRODUCES_JSON_TEXT_HTML_YAML = MediaType.TEXT_HTML+";qs=1,"+APPLICATION_JSON+";qs=0.5,"+APPLICATION_YAML+";qs=0.5";
}
