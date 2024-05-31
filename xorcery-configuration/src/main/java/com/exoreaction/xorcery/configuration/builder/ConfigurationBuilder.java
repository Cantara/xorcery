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
package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.function.Consumer;

/**
 * Configuration builder for the most common cases. If you need anything more than this, use the Configuration.Builder and StandardConfigurationBuilder directly.
 *
 * @param builder
 * @param standardConfigurationBuilder
 */
public record ConfigurationBuilder(Configuration.Builder builder, StandardConfigurationBuilder standardConfigurationBuilder)
{

    public ConfigurationBuilder() {
        this(new Configuration.Builder(), new StandardConfigurationBuilder());
    }

    public ConfigurationBuilder addDefaults()
    {
        standardConfigurationBuilder.addDefaults(builder);
        return this;
    }

    public ConfigurationBuilder addTestDefaults()
    {
        standardConfigurationBuilder.addTestDefaults(builder);
        return this;
    }

    public ConfigurationBuilder addYaml(String yamlString)
    {
        standardConfigurationBuilder.addYaml(yamlString).accept(builder);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationBuilder with(Consumer<Configuration.Builder>... consumers)
    {
        for ( Consumer<Configuration.Builder> consumer : consumers )
        {
            consumer.accept( builder );
        }
        return this;

    }

    public Configuration build()
    {
        return builder.build();
    }
}
