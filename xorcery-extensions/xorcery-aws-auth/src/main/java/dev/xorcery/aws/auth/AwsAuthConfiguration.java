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

import java.util.Optional;

import static dev.xorcery.configuration.Configuration.missing;

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
