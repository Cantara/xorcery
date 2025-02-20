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
package dev.xorcery.keystores;

import dev.xorcery.configuration.Configuration;

import java.io.File;
import java.net.URL;
import java.util.Optional;

import static dev.xorcery.configuration.Configuration.missing;

public record KeyStoreConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseThrow(missing("name"));
    }

    public Optional<String> getPassword() {
        return configuration.getString("password");
    }

    public String getType() {
        return configuration.getString("type").orElse("PKCS12");
    }

    public File getPath() {
        return configuration.getString("path").map(File::new).orElseThrow(missing("path"));
    }

    public boolean isAddRootCa() {
        return configuration.getBoolean("addRootCa").orElse(false);
    }

    public URL getTemplate() {
        return configuration.getResourceURL("template").orElse(null);
    }
}
