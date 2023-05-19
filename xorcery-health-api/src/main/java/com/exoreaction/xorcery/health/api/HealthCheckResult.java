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
package com.exoreaction.xorcery.health.api;

import com.fasterxml.jackson.databind.JsonNode;

public record HealthCheckResult(HealthStatus status, String unhealthyMessage, Throwable unhealthyThrowable, JsonNode info) {

    public HealthCheckResult() {
        this(HealthStatus.HEALTHY, null, null, null);
    }

    public HealthCheckResult(JsonNode info) {
        this(HealthStatus.HEALTHY, null, null, info);
    }

    public HealthCheckResult(String unhealthyMessage) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, null, null);
    }

    public HealthCheckResult(String unhealthyMessage, JsonNode info) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, null, info);
    }

    public HealthCheckResult(Throwable unhealthyThrowable, JsonNode info) {
        this(HealthStatus.UNHEALTHY, "", unhealthyThrowable, info);
    }

    public HealthCheckResult(String unhealthyMessage, Throwable unhealthyThrowable) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, unhealthyThrowable, null);
    }

    public HealthCheckResult(String unhealthyMessage, Throwable unhealthyThrowable, JsonNode info) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, unhealthyThrowable, info);
    }
}
