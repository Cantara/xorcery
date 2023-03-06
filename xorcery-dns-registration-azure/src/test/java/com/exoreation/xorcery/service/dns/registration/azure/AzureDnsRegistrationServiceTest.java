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

import static org.junit.jupiter.api.Assertions.*;

public class AzureDnsRegistrationServiceTest {
    private final String config = """
            jetty.client.enabled: true
            """;

    @Test
    @Disabled
    public void test() throws Exception {
        Configuration configuration = new Configuration.Builder()
            .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
            .build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            var service = xorcery.getServiceLocator().getService(AzureDnsRegistrationService.class);
//            var test = service.login("", "", "");
//            test.whenComplete((azureTokenResponse, throwable) -> {
//                try {
//                    var objectMapper = new JsonMapper();
//                    System.out.println(objectMapper.writeValueAsString(azureTokenResponse));
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//                assertNotNull(azureTokenResponse);
//            });
        }
    }

    @Test
    void test_aRecord() throws JsonProcessingException {
        var objectMapper = new JsonMapper();
        var properties = new AzureDnsRecordRequestProperties.Builder();
        var aRecords = new AzureDnsARecordsRequest.Builder();
        aRecords.aRecord(new AzureDnsARecordRequest.Builder().setIP("1.2.3.4").build());
        properties.aRecords(aRecords.build());
        var metadata = new AzureDnsRequestMetadata.Builder();
        metadata.metadata("test", "test");
        properties.metadata(metadata.build());
        properties.ttl(1);
        var object = new AzureDnsRecordRequest.Builder();
        object.properties(properties.build());


        System.out.println(objectMapper.writeValueAsString(object.build()));
    }

    @Test
    void test_txtRecord() throws JsonProcessingException {
        var objectMapper = new JsonMapper();
        var properties = new AzureDnsRecordRequestProperties.Builder();
        var txtRecords = new AzureDnsTXTRecordsRequest.Builder();
        txtRecords.txtRecord(new AzureDnsTXTRecordRequest.Builder().value("test").build());
        properties.txtRecords(txtRecords.build());
        var metadata = new AzureDnsRequestMetadata.Builder();
        metadata.metadata("test", "test");
        properties.metadata(metadata.build());
        properties.ttl(1);
        var object = new AzureDnsRecordRequest.Builder();
        object.properties(properties.build());
        System.out.println(objectMapper.writeValueAsString(object.build()));
    }

    @Test
    void test_srvRecords() throws JsonProcessingException {
        var objectMapper = new JsonMapper();
        var properties = new AzureDnsRecordRequestProperties.Builder();
        var srvRecords = new AzureDnsSRVRecordsRequest.Builder();
        srvRecords.srvRecord(new AzureDnsSRVRecordRequest.Builder().weight(10).priority(10).port(10).target("test.com").build());
        properties.srvRecords(srvRecords.build());
        var metadata = new AzureDnsRequestMetadata.Builder();
        metadata.metadata("test", "test");
        properties.metadata(metadata.build());
        properties.ttl(1);
        var object = new AzureDnsRecordRequest.Builder();
        object.properties(properties.build());
        System.out.println(objectMapper.writeValueAsString(object.build()));
    }
}
