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
package dev.xorcery.core.hk2;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import org.glassfish.hk2.api.ImmediateController;
import org.glassfish.hk2.runlevel.RunLevelController;

import java.util.Collections;

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

    public String[] getDescriptorNames() {
        return configuration.getListAs("names", JsonNode::textValue).orElse(Collections.emptyList()).toArray(new String[0]);
    }

    public boolean isImmediateScopeEnabled() {
        return configuration.getBoolean("immediateScope.enabled").orElse(true);
    }

    public ImmediateController.ImmediateServiceState getImmediateScopeState() {
        return configuration.getEnum("immediateScope.state", ImmediateController.ImmediateServiceState.class).orElse(ImmediateController.ImmediateServiceState.RUNNING);
    }

    public boolean isThreadScopeEnabled() {
        return configuration.getBoolean("threadScope.enabled").orElse(true);
    }
}
