package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.configuration.Configuration;

import java.util.function.Consumer;

/**
 * Configuration builder for the most common cases. If you need anything more than this, use the Configuration.Builder and StandardConfigurationBuilder directly.
 *
 * @param builder
 * @param standardConfigurationBuilder
 */
public record ConfigurationBuilder(Configuration.Builder builder, StandardConfigurationBuilder standardConfigurationBuilder)
{

    public ConfigurationBuilder() {
        this(new Configuration.Builder(), new StandardConfigurationBuilder());
    }

    public ConfigurationBuilder addDefaults()
    {
        standardConfigurationBuilder.addDefaults(builder);
        return this;
    }

    public ConfigurationBuilder addTestDefaults()
    {
        standardConfigurationBuilder.addTestDefaults(builder);
        return this;
    }

    public ConfigurationBuilder addYaml(String yamlString)
    {
        standardConfigurationBuilder.addYaml(yamlString).accept(builder);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationBuilder with(Consumer<Configuration.Builder>... consumers)
    {
        for ( Consumer<Configuration.Builder> consumer : consumers )
        {
            consumer.accept( builder );
        }
        return this;

    }

    public Configuration build()
    {
        return builder.build();
    }
}
