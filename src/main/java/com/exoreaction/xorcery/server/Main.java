package com.exoreaction.xorcery.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@CommandLine.Command(name="xorcery", version="1.0")
public class Main
    implements Callable<Integer>
{
    @CommandLine.Parameters(index="0", arity = "0..1", description="Configuration file")
    private File configuration;

    @CommandLine.Option(names="-id", description = "Server id")
    private String id;

    @Override
    public Integer call() throws Exception {

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        Logger logger = LogManager.getLogger( Main.class );

        Xorcery xorcery = new Xorcery(configuration, id);

        Runtime.getRuntime().addShutdownHook( new Thread( () ->
        {
            logger.info( "Shutting down server" );
            try
            {
                xorcery.close();
            }
            catch ( Exception e )
            {
                logger.warn( "Error during shutdown", e );
            }
            logger.info( "Shutdown" );

            LogManager.shutdown();
        } ) );

        Thread.currentThread().join();

        return 0;
    }

    public static void main(String[] args ) throws Exception
    {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
