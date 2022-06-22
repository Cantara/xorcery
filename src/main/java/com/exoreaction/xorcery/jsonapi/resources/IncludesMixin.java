package com.exoreaction.xorcery.jsonapi.resources;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface IncludesMixin
    extends ResourceContext
{
    default boolean shouldInclude( Enum<?> type )
    {
        return shouldInclude( type.name() );
    }

    default List<String> includeList()
    {
        String include = getFirstQueryParameter( "include" );
        if ( Strings.isNullOrEmpty( include ) )
        { return Collections.emptyList(); }
        List<String> includes = Arrays.asList( include.split( "," ) );
        return includes;
    }

    default boolean shouldInclude( String type )
    {
        List<String> includes = includeList();
        return includes.contains( type ) ||
                (type.indexOf( '.' ) != -1 && includes.contains( type.substring( type.lastIndexOf( '.' ) ) ));
    }

}
