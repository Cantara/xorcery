package com.exoreaction.xorcery.server.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.server.Xorcery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XorceryTest {

    @Test
    public void thatBasicWiringWorks() throws Exception {
        Configuration configuration = Configuration.Builder.loadTest(null).build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            int httpPort = xorcery.getHttpPort();
            assertTrue(httpPort > 1024);
            System.out.printf("Jetty http port: %d%n", httpPort);
            int httpsPort = xorcery.getHttpsPort();
            assertTrue(httpsPort < 0 || httpsPort > 1024);
            System.out.printf("Jetty https port: %d%n", httpsPort);
        }
    }
}
