package com.exoreaction.reactiveservices.jsonapi.model;

/**
 * @author rickardoberg
 */
public interface JsonApiRels
{
    String self = "self";
    String describedby = "describedby";
    String alternate = "alternate";
    String related = "related";

    // Paging
    String first = "first";
    String prev = "prev";
    String next = "next";
}
