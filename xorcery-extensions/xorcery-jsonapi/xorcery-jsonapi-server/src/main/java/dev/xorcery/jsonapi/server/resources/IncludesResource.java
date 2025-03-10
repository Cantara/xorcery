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
package dev.xorcery.jsonapi.server.resources;

import dev.xorcery.jaxrs.server.resources.ContextResource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface IncludesResource
    extends ContextResource
{
    default boolean shouldInclude( Enum<?> type )
    {
        return shouldInclude( type.name() );
    }

    default List<String> includeList()
    {
        String include = getFirstQueryParameter( "include" );
        if ( include.isBlank() )
        { return Collections.emptyList(); }
        return Arrays.asList( include.split( "," ) );
    }

    default boolean shouldInclude( String type )
    {
        List<String> includes = includeList();
        return includes.contains( type ) ||
                (type.indexOf( '.' ) != -1 && includes.contains( type.substring( type.lastIndexOf( '.' ) ) ));
    }

}
