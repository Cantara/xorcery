package com.exoreaction.reactiveservices.configuration;

import java.io.File;
import java.io.IOException;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */

public record StandardConfiguration(Configuration configuration)
{
    public String home()
    {

        return configuration().getString( "home").orElseGet( ()->
        {
            try
            {
                return new File( ".." ).getCanonicalPath();
            }
            catch ( IOException e )
            {
                return new File("..").getAbsolutePath();
            }
        } );
    }
}
