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
package dev.xorcery.core.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import org.junit.jupiter.api.Test;

public class XorceryTest {

    @Test
    public void runXorcery() throws Exception {
        Configuration configuration = new ConfigurationBuilder().addDefaults().build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            // Start up Xorcery instance
        }
        // Shutdown Xorcery instance
    }
}
