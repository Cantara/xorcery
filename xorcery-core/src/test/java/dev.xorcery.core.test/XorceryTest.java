package dev.xorcery.core.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

public class XorceryTest {

    @Test
    public void runXorcery() throws Exception {
        Configuration configuration = new ConfigurationBuilder().addDefaults().build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            // Start up Xorcery instance
        }
        // Shutdown Xorcery instance
    }
}
