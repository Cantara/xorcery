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
package dev.xorcery.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration builder messages to be logged later by Xorcery
 */
public final class ConfigurationLogger {

    private static final ConfigurationLogger logger = new ConfigurationLogger();

    public static ConfigurationLogger getLogger() {
        return logger;
    }

    private List<String> messages = new ArrayList<>();

    public void log(String message) {
        messages.add(message);
    }

    public List<String> drain() {
        try {
            return messages;
        } finally {
            messages = new ArrayList<>();
        }
    }
}
