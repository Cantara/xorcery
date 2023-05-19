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
package com.exoreaction.xorcery.service.neo4j;

import java.util.Objects;

public record SemanticVersion(int major, int minor, int patch) {

    public static SemanticVersion from(String version) {
        Objects.requireNonNull(version);
        String[] parts = version.split("[.]");
        if (parts.length != 3) {
            throw new IllegalArgumentException("version must be compatible with \"<major>.<minor>.<patch>\"");
        }
        return new SemanticVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public boolean isBreakingChange(SemanticVersion previous) {
        return major != previous.major || minor < previous.minor;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
