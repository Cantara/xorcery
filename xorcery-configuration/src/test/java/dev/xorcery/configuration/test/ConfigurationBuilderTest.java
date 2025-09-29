package dev.xorcery.configuration.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import org.junit.jupiter.api.Test;

public class ConfigurationBuilderTest {

    @Test
    public void testConfigurationBuilder(){
        System.setProperty("XORCERY_SOME_SETTING", "foo,bar");
        Configuration configuration = new ConfigurationBuilder()
                .addYaml("""
                        some:
                            setting: []
                        """)
                .addDefaults().build();

        System.out.println(configuration);
    }
}
