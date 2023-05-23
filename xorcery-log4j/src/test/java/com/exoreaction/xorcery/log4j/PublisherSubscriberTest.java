/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.log4j;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

public class PublisherSubscriberTest {

    private static final String config = """
            reactivestreams.enabled: true
            """;

    @Test
    public void testLog4jPublisherSubscriber() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .add("log4jpublisher.subscriber.authority", "server.xorcery.test:{{ jetty.server.port }}")
                .build();

//        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        try (Xorcery xorcery = new Xorcery(configuration)) {
            LogManager.getLogger(getClass()).info("Test");
            Thread.sleep(1000);
        }
    }

}
