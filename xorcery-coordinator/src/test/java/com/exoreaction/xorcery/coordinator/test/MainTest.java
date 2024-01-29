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
package com.exoreaction.xorcery.coordinator.test;

import com.exoreaction.xorcery.coordinator.Main;
import com.exoreaction.xorcery.core.Xorcery;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.concurrent.CompletableFuture;

@Disabled("only run during development, not in CI")
public class MainTest {

    @Test
    public void testMain() throws InterruptedException {

        Main main = new Main();

        CompletableFuture.runAsync(()->
        {
            new CommandLine(main).execute();
        });

        Xorcery xorcery;
        while ((xorcery = main.getXorcery()) == null)
        {
            Thread.sleep(1000);
        }

        xorcery.close();
    }
}
