package com.exoreaction.reactiveservices.jsonapi.resources;

import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import com.google.common.base.Strings;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FilteringMixin
    extends ResourceContext
{
    // Filters
    default Filters filters()
    {
        return new Filters( getUriInfo().getQueryParameters() );
    }

    default Filters filter( String name, Enum<?> field )
    {
        return filters().filter( name, field );
    }

    default Filters filter( Enum<?> field )
    {
        return filters()
                .filter( "filter[" + Cypher.toField( field ) + "]", field );
    }

    default Filters filter( Enum<?> field, Function<String,Object> valueMapper )
    {
        return filters()
                .filter( "filter[" + Cypher.toField( field ) + "]", field, valueMapper );
    }

    default GraphQuery applyFilters(Map<String,Enum<?>> filters, GraphQuery data )
    {
        filters.forEach( ( k, v ) ->
        {
            String value = getUriInfo().getQueryParameters().getFirst( k );
            if ( !Strings.isNullOrEmpty( value ) )
            {
                data.parameter( v, value );
            }
        } );
        return data;
    }

    default Consumer<GraphQuery> filters(Map<String,Enum<?>> filters )
    {
        return query -> applyFilters( filters, query );
    }

    interface FilterApplier
            extends BiConsumer<String,GraphQuery>
    {
    }

    class FilterParameterApplier
            implements FilterApplier
    {
        private final Enum<?> fieldName;
        private Function<String,Object> vm;

        public FilterParameterApplier( Enum<?> fieldName, Function<String,Object> vm )
        {
            this.fieldName = fieldName;
            this.vm = vm;
        }

        @Override
        public void accept( String value, GraphQuery graphQuery )
        {
            Object parsedValue = vm.apply( value );
            graphQuery.parameter( fieldName, parsedValue );
        }
    }

    class FilterParameterReplaceApplier
            implements FilterApplier
    {
        private final Enum<?> fieldName;
        private final Enum<?> replaces;
        private Function<String,Object> vm;

        public FilterParameterReplaceApplier( Enum<?> fieldName, Enum<?> replaces, Function<String,Object> vm )
        {
            this.fieldName = fieldName;
            this.replaces = replaces;
            this.vm = vm;
        }

        @Override
        public void accept( String value, GraphQuery graphQuery )
        {
            Object parsedValue = vm.apply( value );
            graphQuery.parameter( fieldName, parsedValue );
            graphQuery.getParameters().remove( replaces );
        }
    }
    class Filters
            implements Consumer<GraphQuery>
    {
        private final MultivaluedMap<String,String> queryParameters;
        private final Map<String,FilterApplier> filters = new HashMap<>();

        public Filters( MultivaluedMap<String,String> queryParameters )
        {
            this.queryParameters = queryParameters;
        }

        public Filters filter( String name, Enum<?> field, Function<String,Object> vm )
        {
            filters.put( name, new FilterParameterApplier( field, vm ) );
            return this;
        }

        public Filters filter( String name, Enum<?> field, Enum<?> replacesField, Function<String,Object> vm )
        {
            filters.put( name, new FilterParameterReplaceApplier( field, replacesField, vm ) );
            return this;
        }

        public Filters filter( String name, Enum<?> field )
        {
            return filter( name, field, s -> s );
        }

        public Filters filter( Enum<?> field )
        {
            return filter( "filter[" + Cypher.toField( field ) + "]", field );
        }

        public Filters filter( Enum<?> field, Function<String,Object> vm )
        {
            return filter( "filter[" + Cypher.toField( field ) + "]", field, vm );
        }

        @Override
        public void accept( GraphQuery graphQuery )
        {
            filters.forEach( ( k, v ) ->
            {
                String value = queryParameters.getFirst( k );
                if ( !Strings.isNullOrEmpty( value ) )
                {
                    v.accept( value, graphQuery );
                }
            } );
        }
    }

}
