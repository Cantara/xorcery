package com.exoreaction.reactiveservices.server.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.net.URI;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class Log4jConfigurationFactory extends ConfigurationFactory
{

    static Configuration createConfiguration( final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel( Level.ERROR);
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL).
                           addAttribute("level", Level.INFO));

        {
            AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").
                                                              addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
            appenderBuilder.add(builder.newLayout("PatternLayout").
                                       addAttribute("pattern", "%d [%t] %-5level %logger{36}: %msg%n%throwable"));
            appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY,
                Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
            builder.add(appenderBuilder);
        }

        {
            AppenderComponentBuilder appenderBuilder = builder.newAppender("Disruptor", "DISRUPTOR");
            appenderBuilder.add(builder.newLayout("JsonTemplateLayout"));
            appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY,
                Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
            builder.add(appenderBuilder);
        }

        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG).
                           add(builder.newAppenderRef("Stdout")).
                           add(builder.newAppenderRef("Disruptor")).
                           addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.ERROR)
//                           .add(builder.newAppenderRef("Stdout"))
                           .add(builder.newAppenderRef("Disruptor")));
        return builder.build();
    }

    @Override
    public Configuration getConfiguration( final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[] {"*"};
    }
}