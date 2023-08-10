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
package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.WithContext;
import com.exoreaction.xorcery.configuration.ApplicationConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;

public interface DeploymentMetadata
        extends WithContext<Metadata> {

    interface Builder<T> {
        Metadata.Builder builder();

        default T configuration(Configuration configuration) {
            InstanceConfiguration instanceConfiguration = InstanceConfiguration.get(configuration);
            ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.get(configuration);
            builder().add("environment", instanceConfiguration.getEnvironment())
                    .add("tag", instanceConfiguration.getTag())
                    .add("name", applicationConfiguration.getName())
                    .add("version", applicationConfiguration.getVersion())
                    .add("host", instanceConfiguration.getHost());
            return (T) this;
        }
    }

    default String getEnvironment() {
        return context().getString("environment").orElse("default");
    }

    default String getTag() {
        return context().getString("tag").orElse("default");
    }

    default String getVersion() {
        return context().getString("version").orElse("0.1.0");
    }

    default String getName() {
        return context().getString("name").orElse("noname");
    }

    default String getHost() {
        return context().getString("host").orElse("localhost");
    }
}
