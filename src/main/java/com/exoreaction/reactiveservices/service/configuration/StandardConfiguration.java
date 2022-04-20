package com.exoreaction.reactiveservices.service.configuration;

import java.io.File;
import java.io.IOException;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */

public class StandardConfiguration
{
    private final Configuration configuration;

    public StandardConfiguration( Configuration configuration )
    {
        this.configuration = configuration;
    }

    public String home()
    {
        String home;
        try
        {
            home = new File( ".." ).getCanonicalPath();
        }
        catch ( IOException e )
        {
            home = new File("..").getAbsolutePath();
        }

        return configuration.getString( "home", home );
    }
}
