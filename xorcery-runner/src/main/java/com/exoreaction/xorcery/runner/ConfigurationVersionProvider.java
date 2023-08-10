package com.exoreaction.xorcery.runner;

import com.exoreaction.xorcery.configuration.ApplicationConfiguration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import picocli.CommandLine;

public class ConfigurationVersionProvider
    implements CommandLine.IVersionProvider
{
    @Override
    public String[] getVersion() throws Exception {

        return ApplicationConfiguration.get(new ConfigurationBuilder().addDefaults().build()).getVersion().split("\\.");
    }
}
