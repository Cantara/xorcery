package dev.xorcery.jwt.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public record UserConfiguration(List<ConfigurationUser> users) {
    public static UserConfiguration get(Configuration configuration)
    {
        return new UserConfiguration(configuration.getObjectListAs("jwt.users",ConfigurationUser::new).orElse(Collections.emptyList()));
    }

    public Optional<ConfigurationUser> getUser(String username) {
        for (ConfigurationUser user : users) {
            if (Objects.equals(user.getName(), username))
                return Optional.of(user);
        }
        return Optional.empty();
    }

    public record ConfigurationUser(JsonNode json)
            implements JsonElement {

        public String getName()
        {
            return getString("name").orElse(null);
        }

        public Optional<String> getPassword()
        {
            return getString("password");
        }

        public ObjectNode getClaims()
        {
            return getObjectAs("claims", Function.identity()).orElse(JsonNodeFactory.instance.objectNode());
        }
    }
}
