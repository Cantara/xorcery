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
package dev.xorcery.concurrent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
class SmartBatcherTest {
    
    @Test
    public void testSmartBatcher() throws InterruptedException {
        try (SmartBatcher<String> batcher = new SmartBatcher<>(this::handler, new ArrayBlockingQueue<>(1024), Executors.newSingleThreadExecutor()))
        {
            for (int i = 0; i < 1000; i++) {
                batcher.submit("value"+i);
                Thread.sleep(1);
            }
            System.out.println("Close");
        }
        System.out.println("SmartBatcher closed");
        Assertions.assertEquals(1000, count);
    }

    int count;

    private void handler(Collection<String> strings) {
        try {
            Thread.sleep(100);
            int size = strings.size();
            count += size;
            System.out.println("Handled "+size+":"+count);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}