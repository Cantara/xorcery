package com.exoreaction.xorcery.neo4j.jsonapi.resources;

import com.exoreaction.xorcery.jsonapi.server.resources.ResourceContext;
import com.exoreaction.xorcery.service.neo4j.client.GraphQuery;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface FieldsMixin
    extends ResourceContext
{
    // Field selection
    default Predicate<String> getFieldSelection(String type )
    {
        return getFieldSelection( type, true, true );
    }

    default Predicate<String> getFieldSelection( String type, boolean trueOnMissing, boolean trueOnEmpty )
    {
        Predicate<String> fieldSelection;
        String fields = getUriInfo().getQueryParameters().getFirst( "fields[" + type + "]" );

        if ( fields == null )
        {
            fieldSelection = k -> trueOnMissing;
        }
        else if ( fields.equals( "" ) )
        {
            fieldSelection = k -> trueOnEmpty;
        }
        else
        {
            Set<String> fieldList = Set.of( fields.split( "," ) );
            fieldSelection = fieldList::contains;
        }
        return fieldSelection;
    }

    default Predicate<String> getFieldSelection( Enum<?> type )
    {
        return getFieldSelection( type.name().toLowerCase() );
    }

    default Predicate<String> getFieldPrefixSelection( String prefix )
    {
        return field -> field.startsWith( prefix );
    }

    default Consumer<GraphQuery> selectedFields(String... prefixes )
    {
        return query ->
        {
            for ( String prefix : prefixes )
            {
                Predicate<String> fieldFilter = getFieldSelection( prefix );

                query.getResults().removeIf( anEnum -> anEnum.getClass().getSimpleName().toLowerCase().equals( prefix ) &&
                        !fieldFilter.test( prefix + "_" + anEnum.name() ) );
            }
        };
    }

    default Consumer<GraphQuery> selectedFields( Enum<?>... prefixes )
    {
        return query ->
        {
            for ( Enum<?> prefix : prefixes )
            {
                String prefixName = prefix.name().toLowerCase();
                Predicate<String> fieldFilter = getFieldSelection( prefixName );

                query.getResults()
                        .removeIf( anEnum -> anEnum.getClass().getSimpleName().toLowerCase().equals( prefixName ) &&
                                !fieldFilter.test( prefixName + "_" + anEnum.name() ) );
            }
        };
    }
}
