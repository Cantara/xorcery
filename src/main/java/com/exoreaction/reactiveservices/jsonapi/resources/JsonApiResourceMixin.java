package com.exoreaction.reactiveservices.jsonapi.resources;

import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.Context;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import jakarta.ws.rs.core.UriBuilder;

import java.util.function.Consumer;

/**
 * Helper methods for JSON:API resource implementations
 */
public interface JsonApiResourceMixin
    extends ModelsMixin, CommandsMixin, FilteringMixin, FieldsMixin, IncludesMixin, SortingMixin, PagingMixin
{
}
