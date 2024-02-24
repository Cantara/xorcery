package com.exoreaction.xorcery.aws.auth;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

@Service(name = "aws.auth")
public class CredentialsProviderFactory
        implements Factory<AwsCredentialsProvider> {
    private final AwsCredentialsProvider credentialsProvider;

    @Inject
    public CredentialsProviderFactory(Configuration configuration, Secrets secrets) {
        AwsAuthConfiguration awsAuthConfiguration = AwsAuthConfiguration.get(configuration, secrets);

        credentialsProvider = switch (awsAuthConfiguration.getAuthType()) {
            case accessKey -> LazyAwsCredentialsProvider.create(() ->
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(
                            awsAuthConfiguration.getAccessKeyId().orElseThrow(missing("accessKeyId")),
                            awsAuthConfiguration.getSecretAccessKey().orElseThrow(missing("secretAccessKey")))));
            case sessionToken -> LazyAwsCredentialsProvider.create(() ->
                    StaticCredentialsProvider.create(AwsSessionCredentials.create(
                            awsAuthConfiguration.getAccessKeyId().orElseThrow(missing("accessKeyId")),
                            awsAuthConfiguration.getSecretAccessKey().orElseThrow(missing("secretAccessKey")),
                            awsAuthConfiguration.getSessionToken().orElseThrow(missing("sessionToken"))
                    )));
            case container -> ContainerCredentialsProvider.builder()
                    .build();
            case instance -> InstanceProfileCredentialsProvider.builder()
                    .profileFile(ProfileFile.defaultProfileFile())
                    .build();
        };
    }

    @Override
    @Singleton
    public AwsCredentialsProvider provide() {
        return credentialsProvider;
    }

    @Override
    public void dispose(AwsCredentialsProvider instance) {
    }
}
