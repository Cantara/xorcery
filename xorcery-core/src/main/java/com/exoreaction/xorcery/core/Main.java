package com.exoreaction.xorcery.core;

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

    @CommandLine.Option(names="-log4j", description = "Log4j configuration")
    private File log4jConfiguration;

    @Override
    public Integer call() throws Exception {

        // Allow for additive log4j configuration file
        if (log4jConfiguration != null && log4jConfiguration.exists())
        {
            String log4jProperty = System.getProperty("log4j2.configurationFile");
            System.setProperty("log4j2.configurationFile", log4jProperty == null ?
                    "log4j2.yaml,"+log4jConfiguration.getAbsolutePath() :
                    log4jProperty+","+log4jConfiguration.getAbsolutePath());
        }

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        Logger logger = LogManager.getLogger( Main.class );

        if (id != null)
            System.setProperty("server_id", id);

        Xorcery xorcery = new Xorcery(configuration);

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
