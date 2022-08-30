package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.server.Xorcery;
import org.junit.jupiter.api.Test;

public class ServerSubscriberIT {

    private static final String config = """
            eventclient.enabled: true
            eventserver.enabled: true
            registry.enabled: true
            conductor:
                enabled: true
                templates:
                  - id: clientpublisher_serversubscriber
                    type: grouptemplate
                    attributes:
                      name: Client publishes events to server subscriber
                      sources:
                        pattern: rel=='eventsubscriber'
                      consumers:
                        pattern: type=='eventclient'
                            
            reactivestreams.enabled: true
            """;

    @Test
    public void givenSubscriberWhenPublishEventsThenSubscriberConsumesEvents() throws Exception {
        try (Xorcery xorcery = new Xorcery(Configuration.Builder.loadTest(null).addYaml(config).build()))
        {
            ClientPublisherService clientPublisherService = xorcery.getInjectionManager().getInstance(ClientPublisherService.class);

            clientPublisherService.getDone().get();
        }
    }
}
