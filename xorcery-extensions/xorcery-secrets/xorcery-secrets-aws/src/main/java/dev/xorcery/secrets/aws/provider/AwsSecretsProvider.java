package dev.xorcery.secrets.aws.provider;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.secrets.spi.SecretsProvider;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.xorcery.configuration.Configuration.missing;

@Service(name="aws", metadata = "enabled=secrets.aws")
@ContractsProvided({SecretsProvider.class})
public class AwsSecretsProvider
    implements SecretsProvider, PreDestroy
{
    private final SecretsManagerClient secretsClient;
    private final Map<String, String> cachedSecrets = new ConcurrentHashMap<>();

    @Inject
    public AwsSecretsProvider(AwsCredentialsProvider awsCredentialsProvider, Configuration configuration) {
        secretsClient = SecretsManagerClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(configuration.getString("secrets.aws.region").orElseThrow(missing("secrets.aws.region"))))
                .build();
    }

    @Override
    public String getSecretString(String name) throws UncheckedIOException, IllegalArgumentException {
        return cachedSecrets.computeIfAbsent(name, this::getSecretsManagerString);
    }

    @Override
    public void refreshSecret(String name) throws UncheckedIOException, IllegalArgumentException {
        cachedSecrets.computeIfPresent(name, (n,v)->getSecretsManagerString(n));
    }

    private String getSecretsManagerString(String name) {
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(name)
                    .build();
            GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
            return valueResponse.secretString();
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException("Secret '"+ name +"' not found", e);
        }
    }

    @Override
    public void preDestroy() {
        secretsClient.close();
    }
}
