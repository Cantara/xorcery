package com.exoreaction.xorcery.configuration.builder.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import org.junit.jupiter.api.Test;

public class ResourceConfigurationProviderTest {

    @Test
    public void testResource()
    {
        Configuration configuration = new ConfigurationBuilder()
                .addConfigurationProviders()
                .addYaml("""
foo: "{{ RESOURCE.META-INF/foo.yaml }}"
bar: "{{ RESOURCE.META-INF/bar.json }}"
                        """).build();

        System.out.println(configuration);
    }
}
