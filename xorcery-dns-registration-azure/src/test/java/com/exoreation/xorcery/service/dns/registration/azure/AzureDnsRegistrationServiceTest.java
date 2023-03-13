package com.exoreation.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.dns.registration.azure.AzureDnsRegistrationService;
import com.exoreaction.xorcery.service.dns.registration.azure.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AzureDnsRegistrationServiceTest {
    private final String config = """
            jetty.client.enabled: true
            jetty.client.ssl.enabled: true
            jetty.client.http2.enabled: false
            dns:
              registration:
                azure:
                  tenantId: 
                  clientId: 
                  clientSecret: 
                  subscription: 
                  resourceGroup: 
                  zone: 
            """;

    @Test
    @Disabled
    public void test() throws Exception {
        Configuration configuration = new Configuration.Builder()
            .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
            .build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            var service = xorcery.getServiceLocator().getService(AzureDnsRegistrationService.class);
        }
    }

    @Test
    @Disabled
    void test_aRecord() throws JsonProcessingException {
        var objectMapper = new JsonMapper();
        var properties = new AzureDnsRecordProperties.Builder();
        var aRecords = new AzureDnsARecords.Builder();
        aRecords.aRecord(new AzureDnsARecord.Builder().setIP("1.2.3.4").build());
        properties.aRecords(aRecords.build());
        var metadata = new AzureDnsMetadata.Builder();
        metadata.metadata("test", "test");
        properties.metadata(metadata.build());
        properties.ttl(1);
        var object = new AzureDnsRecord.Builder();
        object.properties(properties.build());


        System.out.println(objectMapper.writeValueAsString(object.build()));
    }

    @Test
    @Disabled
    void test_txtRecord() throws JsonProcessingException {
        var objectMapper = new JsonMapper();
        var properties = new AzureDnsRecordProperties.Builder();
        var txtRecords = new AzureDnsTXTRecords.Builder();
        txtRecords.txtRecord(new AzureDnsTXTRecord.Builder().value("test").build());
        properties.txtRecords(txtRecords.build());
        var metadata = new AzureDnsMetadata.Builder();
        metadata.metadata("test", "test");
        properties.metadata(metadata.build());
        properties.ttl(1);
        var object = new AzureDnsRecord.Builder();
        object.properties(properties.build());
        System.out.println(objectMapper.writeValueAsString(object.build()));
    }

    @Test
    @Disabled
    void test_srvRecords() throws JsonProcessingException {
        var objectMapper = new JsonMapper();
        var properties = new AzureDnsRecordProperties.Builder();
        var srvRecords = new AzureDnsSRVRecords.Builder();
        srvRecords.srvRecord(new AzureDnsSRVRecord.Builder().weight(10).priority(10).port(10).target("test.com").build());
        properties.srvRecords(srvRecords.build());
        var metadata = new AzureDnsMetadata.Builder();
        metadata.metadata("test", "test");
        properties.metadata(metadata.build());
        properties.ttl(1);
        var object = new AzureDnsRecord.Builder();
        object.properties(properties.build());
        System.out.println(objectMapper.writeValueAsString(object.build()));
    }
}
