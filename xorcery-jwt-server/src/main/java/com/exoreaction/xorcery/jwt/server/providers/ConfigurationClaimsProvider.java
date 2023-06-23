package com.exoreaction.xorcery.jwt.server.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jwt.server.spi.ClaimsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;

@Service(name = "jwt.server.claims.configuration")
@ContractsProvided(ClaimsProvider.class)
public class ConfigurationClaimsProvider
        implements ClaimsProvider {
    private final Configuration users;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ConfigurationClaimsProvider(Configuration configuration) {
        users = configuration.getConfiguration("jwt.users");
    }

    @Override
    public Map<String, ?> getClaims(String userName) {
        return users.getJson(userName).map(node -> {
            try {
                return objectMapper.treeToValue(node, Map.class);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }).orElse(Collections.emptyMap());
    }
}
