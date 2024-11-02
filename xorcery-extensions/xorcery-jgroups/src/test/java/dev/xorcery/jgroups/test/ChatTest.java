package dev.xorcery.jgroups.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Disabled
public class ChatTest {

    static String config = """
            foo: bar
            """;

    @RegisterExtension
    static XorceryExtension server1 = XorceryExtension.xorcery()
            .id("server1")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .build();

    @RegisterExtension
    static XorceryExtension server2 = XorceryExtension.xorcery()
            .id("server2")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .build();

    @Test
    public void testChatBots() throws InterruptedException {
        Thread.sleep(30000);
    }
}
