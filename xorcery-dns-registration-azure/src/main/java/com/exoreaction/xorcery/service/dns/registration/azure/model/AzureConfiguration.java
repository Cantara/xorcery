package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.WithContext;
import com.exoreaction.xorcery.configuration.model.Configuration;

public interface AzureConfiguration extends WithContext<Configuration> {
    default String getTenantId() {
        return context().getString("tenantId")
                .orElseThrow(() -> new IllegalArgumentException("Missing tenant id"));
    }

    default String getClientId() {
        return context().getString("clientId")
                .orElseThrow(() -> new IllegalArgumentException("Missing client id"));
    }

    default String getClientSecret() {
        return context().getString("clientSecret")
                .orElseThrow(() -> new IllegalArgumentException("Missing client secret"));
    }

    default String getSubscription() {
        return context().getString("subscription")
                .orElseThrow(() -> new IllegalArgumentException("Missing subscription"));
    }

    default String getResourceGroup() {
        return context().getString("resourceGroup")
                .orElseThrow(() -> new IllegalArgumentException("Missing resource group"));
    }

    default String getZone() {
        return context().getString("zone")
                .orElseThrow(() -> new IllegalArgumentException("Missing zone"));
    }
}
