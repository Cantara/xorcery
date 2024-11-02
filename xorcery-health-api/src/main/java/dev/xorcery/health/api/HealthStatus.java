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
package dev.xorcery.health.api;

public enum HealthStatus {
    HEALTHY(true),
    INITIALIZING(false),
    UNHEALTHY(false),
    PARTIALLY_HEALTHY(false);

    private final boolean healthy;

    HealthStatus(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean healthy() {
        return healthy;
    }
}
