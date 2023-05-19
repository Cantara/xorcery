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
package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URL;

public record KeyStoreConfiguration(String name, Configuration configuration) {
    public URL getURL() {
        return configuration.getResourceURL("path").orElseThrow(() -> new IllegalArgumentException("Missing " + name + ".path"));
    }

    public char[] getPassword() {
        return configuration.getString("password").map(String::toCharArray).orElse(null);
    }

    public String getType() {
        return configuration.getString("type").orElse("PKCS12");
    }

    public String getPath() {
        return configuration.getString("path").orElseThrow(() -> new IllegalArgumentException("Missing " + name + ".path"));
    }

    public boolean isAddRootCa() {
        return configuration.getBoolean("addRootCa").orElse(false);
    }

    public String getTemplate() {
        return configuration.getString("template").orElse(null);
    }
}
