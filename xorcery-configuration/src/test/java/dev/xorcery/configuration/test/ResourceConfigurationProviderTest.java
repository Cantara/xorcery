package dev.xorcery.configuration.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import org.junit.jupiter.api.Test;

public class ResourceConfigurationProviderTest {

    @Test
    public void testResource()
    {
        Configuration configuration = new ConfigurationBuilder()
                .addConfigurationProviders()
                .addYaml("""
foo: "{{ RESOURCE.yaml.META-INF/foo.yaml }}"
bar: "{{ RESOURCE.json.META-INF/bar.json }}"
string: "{{ RESOURCE.string.META-INF/bar.json }}"
                        """).build();

        System.out.println(configuration);
    }
}
