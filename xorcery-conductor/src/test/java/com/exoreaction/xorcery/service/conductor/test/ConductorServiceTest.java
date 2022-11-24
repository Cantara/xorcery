package com.exoreaction.xorcery.service.conductor.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.util.Sockets;
import com.exoreaction.xorcery.service.conductor.api.Group;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.junit.jupiter.api.Test;
import org.jvnet.hk2.annotations.Service;

public class ConductorServiceTest {

    @Test
    public void testConductor() throws Exception {

        String yaml = """
conductor.templates:
    - grouptemplates.json                
                """;

        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(yaml))
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
        }
    }

    @MessageReceiver
    @Service
    public static class GroupTestListener
    {
        public GroupTestListener() {
            System.out.println("TEST");
        }

        public void newGroup(@SubscribeTo Group group)
        {
            LogManager.getLogger(ConductorServiceTest.class).info("Group:"+group);
        }

        public void newGroupTemplate(@SubscribeTo GroupTemplate groupTemplate)
        {
            LogManager.getLogger(ConductorServiceTest.class).info("Group template:"+groupTemplate);

        }
    }
}
