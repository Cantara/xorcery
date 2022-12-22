package com.exoreaction.xorcery.service.dns.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class SRVResolverTest {

    Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testClientSRVResolverLoadBalancing() throws Exception {
        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration1 = new Configuration.Builder()
                .with(standardConfigurationBuilder.addTestDefaultsWithYaml("""
                        name: xorcery1
                        server.http.port: 8888
                        dns:
                            hosts:
                                analytics:
                                    - 127.0.0.1:8888
                                    - 127.0.0.1:8080
                            server:
                                enabled: true
                        """)).build();
        Configuration configuration2 = new Configuration.Builder()
                .with(standardConfigurationBuilder.addTestDefaultsWithYaml("""
                        name: xorcery2
                        server.http.port: 8888
                        """)).build();
        logger.info("Resolved configuration1:\n" + StandardConfigurationBuilder.toYaml(configuration1));
        logger.info("Resolved configuration2:\n" + StandardConfigurationBuilder.toYaml(configuration2));

        try (Xorcery xorcery1 = new Xorcery(configuration1)) {
            try (Xorcery xorcery2 = new Xorcery(configuration2)) {
                ClientConfig clientConfig = xorcery2.getServiceLocator().getService(ClientConfig.class);
                Client client = ClientBuilder.newClient(clientConfig
                        .register(JsonElementMessageBodyReader.class)
                );

                for (int i = 0; i < 1; i++) {
//                    System.out.println(client.target("http://analytics/").request(MediaTypes.APPLICATION_JSON_API_TYPE).get().readEntity(String.class));
                    ResourceDocument resourceDocument = client.target("http://analytics/").request().get().readEntity(ResourceDocument.class);
                    System.out.println(resourceDocument.getLinks().getByRel("self").orElse(null).getHrefAsUri());
                    System.out.println(resourceDocument.getMeta().getMeta().toPrettyString());
//                    MultivaluedMap<String, Object> headers = client.target("http://analytics").request().get().getHeaders();
//                    System.out.println(headers);
//                    Thread.sleep(1000);
                }
            }
        }
    }

    @Test
    public void testClientSRVResolverFailover() throws Exception {
        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration2 = new Configuration.Builder()
                .with(standardConfigurationBuilder.addTestDefaultsWithYaml("""
                        name: xorcery1
                        server.http.port: 8888
                        dns:
                            enabled: true
                            hosts:
                                analytics:
                                    - 127.0.0.1:8888
                                    - 127.0.0.1:8080
                            server:
                                enabled: true
                        """)).build();
//        logger.info("Resolved configuration2:\n" + standardConfigurationBuilder.toYaml(configuration2));

        try (Xorcery xorcery2 = new Xorcery(configuration2)) {
            ClientConfig clientConfig = xorcery2.getServiceLocator().getService(ClientConfig.class);
            Client client = ClientBuilder.newClient(clientConfig
                    .register(JsonElementMessageBodyReader.class)
            );

            for (int i = 0; i < 100; i++) {
                ResourceDocument resourceDocument = client.target("http://analytics").request().get().readEntity(ResourceDocument.class);
                System.out.println(resourceDocument.getLinks().getByRel("self").orElse(null).getHrefAsUri());
                System.out.println(resourceDocument.getMeta().getMeta().toPrettyString());
//                    MultivaluedMap<String, Object> headers = client.target("http://analytics").request().get().getHeaders();
//                    System.out.println(headers);
//                    Thread.sleep(1000);
            }
        }
    }
}
