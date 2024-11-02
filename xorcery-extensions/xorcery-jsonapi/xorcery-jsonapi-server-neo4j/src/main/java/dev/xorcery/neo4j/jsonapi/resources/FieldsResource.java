/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.neo4j.jsonapi.resources;

import dev.xorcery.jaxrs.server.resources.ContextResource;
import dev.xorcery.neo4j.client.GraphQuery;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface FieldsResource
    extends ContextResource
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
