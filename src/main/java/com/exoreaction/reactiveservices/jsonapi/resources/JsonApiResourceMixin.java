package com.exoreaction.reactiveservices.jsonapi.resources;

/**
 * Helper methods for JSON:API resource implementations
 */
public interface JsonApiResourceMixin
    extends ModelsMixin, RelationshipsMixin, CommandsMixin,
        FilteringMixin, FieldsMixin, IncludesMixin, SortingMixin, PagingMixin,
        JsonSchemaMixin
{
}
