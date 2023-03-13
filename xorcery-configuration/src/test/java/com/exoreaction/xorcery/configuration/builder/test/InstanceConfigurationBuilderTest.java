package com.exoreaction.xorcery.configuration.builder.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import org.junit.jupiter.api.Test;

public class InstanceConfigurationBuilderTest {

    @Test
    public void testDefaults()
    {
        Configuration configuration = new Configuration.Builder().with(new StandardConfigurationBuilder()::addDefaults).build();

        System.out.println(StandardConfigurationBuilder.toYaml(configuration));
    }
}
