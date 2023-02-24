package com.exoreaction.xorcery.jsonapi;

public class MediaTypes
{
    public final static String APPLICATION_JSON_API = "application/vnd.api+json";
    public final static String APPLICATION_YAML = "application/yaml";
    public static final String APPLICATION_JSON_SCHEMA = "application/schema+json";

    // For resources that produce JSON-API ResourceDocument that can be also be rendered as HTML or YAML
    public static final String PRODUCES_JSON_API_TEXT_HTML_YAML = "text/html"+";qs=1,"+APPLICATION_JSON_API+";qs=0.5,"+"application/json"+";qs=0.4,"+APPLICATION_YAML+";qs=0.3";

    // For resources that produce JSON that can be also be rendered as HTML or YAML
    public static final String PRODUCES_JSON_TEXT_HTML_YAML = "text/html"+";qs=1,"+"application/json"+";qs=0.5,"+APPLICATION_YAML+";qs=0.5";
}
