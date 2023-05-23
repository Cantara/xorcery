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
package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.Configuration;
import org.glassfish.hk2.runlevel.RunLevelController;

public record Hk2Configuration(Configuration configuration) {

    public RunLevelController.ThreadingPolicy getThreadingPolicy() {
        return RunLevelController.ThreadingPolicy.valueOf(configuration.getString("threadPolicy").orElse("FULLY_THREADED"));
    }

    public int getMaximumUseableThreads() {
        return configuration.getInteger("threadCount").orElse(5);
    }

    public int getRunLevel() {
        return configuration.getInteger("runLevel").orElse(20);
    }
}
