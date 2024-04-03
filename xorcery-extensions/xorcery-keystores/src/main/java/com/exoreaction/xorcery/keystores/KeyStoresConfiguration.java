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
package com.exoreaction.xorcery.keystores;

import com.exoreaction.xorcery.configuration.Configuration;

public record KeyStoresConfiguration(Configuration configuration) {

    public static KeyStoresConfiguration get(Configuration configuration)
    {
        return new KeyStoresConfiguration(configuration.getConfiguration("keystores"));
    }

    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public KeyStoreConfiguration getKeyStoreConfiguration(String name)
    {
        return new KeyStoreConfiguration(name, configuration.getConfiguration(name));
    }
}