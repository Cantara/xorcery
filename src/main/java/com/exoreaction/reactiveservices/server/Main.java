package com.exoreaction.reactiveservices.server;

import com.exoreaction.reactiveservices.server.log4j.Log4jConfigurationFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

public class Main
{
    public static void main( String[] args ) throws Exception
    {
        ConfigurationFactory.setConfigurationFactory( new Log4jConfigurationFactory() );
        Logger logger = LogManager.getLogger( Main.class );

        Server server = new Server();

        Runtime.getRuntime().addShutdownHook( new Thread( () ->
        {
            try
            {
                server.close();
            }
            catch ( Exception e )
            {
                logger.warn( "Error during shutdown", e );
            }
            logger.info( "Shutdown" );

            LogManager.shutdown();
        } ) );

        Thread.currentThread().join();
    }
}
