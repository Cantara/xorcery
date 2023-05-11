package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private Logger logger = LogManager.getLogger( Main.class );

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

        System.setProperty("log4j2.isThreadContextMapInheritable", "true");

        if (id != null)
            System.setProperty("server_id", id);

        Configuration configuration = loadConfiguration();

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

    protected Configuration loadConfiguration() throws JsonProcessingException {
        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration.Builder builder = new Configuration.Builder()
                .with(standardConfigurationBuilder::addDefaults, standardConfigurationBuilder.addFile(configuration));

        // Log final configuration
        logger.debug("Configuration:\n" + StandardConfigurationBuilder.toYaml(builder));

        Configuration configuration = builder.build();
        logger.info("Resolved configuration:\n" + StandardConfigurationBuilder.toYaml(configuration));
        return configuration;
    }

    public static void main(String[] args ) throws Exception
    {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        System.exit(new CommandLine(new Main()).execute(args));
    }
}
