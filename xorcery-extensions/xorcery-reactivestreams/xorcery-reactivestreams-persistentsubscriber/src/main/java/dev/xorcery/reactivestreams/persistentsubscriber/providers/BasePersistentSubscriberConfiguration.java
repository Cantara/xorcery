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
package dev.xorcery.reactivestreams.persistentsubscriber.providers;

import dev.xorcery.configuration.Configuration;

import java.util.Optional;

public record BasePersistentSubscriberConfiguration(Configuration configuration) {
    // if true in configuration, skip until System.currentTimeMillis()
    public boolean getSkipOld() {
        return configuration.getBoolean("skipOld").orElse(false);
    }

    // if set to long timestamp, skip until this timestamp is seen in event metadata
    public Optional<Long> getSkipUntil() {
        return configuration.getLong("skipUntil");
    }
}
