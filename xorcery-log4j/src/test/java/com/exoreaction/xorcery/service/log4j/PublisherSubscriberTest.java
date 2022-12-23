package com.exoreaction.xorcery.service.log4j;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class PublisherSubscriberTest {

    private static final String config = """
            reactivestreams.enabled: true
            """;

    @Test
    public void testLog4jPublisherSubscriber() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("server.http.port", Sockets.nextFreePort())
                .add("server.ssl.port", Sockets.nextFreePort())
                .add("log4jpublisher.subscriber.authority", "server.xorcery.test:{{ server.port }}")
                .build();

        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            LogManager.getLogger(getClass()).info("Test");
            Thread.sleep(1000);
        }
    }

}
