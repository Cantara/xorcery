package com.exoreaction.reactiveservices.jaxrs;

import jakarta.ws.rs.core.MediaType;

public class MediaTypes
{
    public final static String APPLICATION_JSON_API = "application/vnd.api+json";
    public final static String APPLICATION_JSON_LOGEVENT = "application/vnd.logevent+json";

    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JSON_API} media type.
     */
    public final static MediaType APPLICATION_JSON_API_TYPE =
            new MediaType("application", "vnd.api+json");

    // For resources that produce JSON-API ResourceDocument that can be rendered as HTML
    public static final String JSON_API_TEXT_HTML = MediaType.TEXT_HTML+"; qs=0.9,"+APPLICATION_JSON_API;

//    public static final String PRODUCES_JSON_SCHEMA = JsonSchema.JSON_SCHEMA + ";qs=0.1";
}
