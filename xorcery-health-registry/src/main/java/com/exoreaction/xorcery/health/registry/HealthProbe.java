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
package com.exoreaction.xorcery.health.registry;

import java.util.function.Supplier;

public class HealthProbe {
    final String key;
    final Supplier<Object> probe;

    public HealthProbe(String key, Supplier<Object> probe) {
        this.key = key;
        this.probe = probe;
    }

    public String getKey() {
        return key;
    }

    public Supplier<Object> getProbe() {
        return probe;
    }
}
