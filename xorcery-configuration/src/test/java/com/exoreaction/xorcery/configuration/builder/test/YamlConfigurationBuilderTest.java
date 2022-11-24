package com.exoreaction.xorcery.configuration.builder.test;

import com.exoreaction.xorcery.configuration.builder.YamlConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import org.junit.jupiter.api.Test;

public class YamlConfigurationBuilderTest {


    @Test
    public void testMergeDuplicatedEntries()
    {
        String config = """
                foo:
                    - bar
                foo:
                    - xyzzy                       
                """;

        Configuration configuration = new Configuration.Builder().with(b -> new YamlConfigurationBuilder(b).addYaml(config)).build();
        System.out.println(configuration);
    }
}
