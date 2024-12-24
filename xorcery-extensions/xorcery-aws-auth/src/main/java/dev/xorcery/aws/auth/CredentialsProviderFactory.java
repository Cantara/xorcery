/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.aws.auth;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;

import static dev.xorcery.configuration.Configuration.missing;

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
