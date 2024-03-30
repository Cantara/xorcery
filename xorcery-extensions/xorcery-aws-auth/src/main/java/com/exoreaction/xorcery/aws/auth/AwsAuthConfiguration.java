package com.exoreaction.xorcery.aws.auth;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;

import java.util.Optional;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record AwsAuthConfiguration(Configuration configuration, Secrets secrets) {

    public static AwsAuthConfiguration get(Configuration configuration, Secrets secrets)
    {
        return new AwsAuthConfiguration(configuration.getConfiguration("aws.auth"), secrets);
    }

    public AuthType getAuthType()
    {
        return configuration.getEnum("type", AuthType.class).orElseThrow(missing("type"));
    }

    public Optional<String> getAccessKeyId()
    {
        return configuration.getString("accessKeyId").map(secrets::getSecretString);
    }

    public Optional<String> getSecretAccessKey()
    {
        return configuration.getString("secretAccessKey").map(secrets::getSecretString);
    }

    public Optional<String> getSessionToken()
    {
        return configuration.getString("sessionToken").map(secrets::getSecretString);
    }

    public enum AuthType
    {
        accessKey,
        sessionToken,
        container,
        instance
        // Expand this list as needed
    }
}
