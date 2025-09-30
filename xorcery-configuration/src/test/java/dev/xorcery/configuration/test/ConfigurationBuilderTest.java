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
package dev.xorcery.configuration.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import org.junit.jupiter.api.Test;

public class ConfigurationBuilderTest {

    @Test
    public void testConfigurationBuilder(){
        System.setProperty("XORCERY_SOME_SETTING", "foo,bar");
        Configuration configuration = new ConfigurationBuilder()
                .addYaml("""
                        some:
                            setting: []
                        """)
                .addDefaults().build();

        System.out.println(configuration);
    }
}
