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
package dev.xorcery.concurrent.test;

import dev.xorcery.concurrent.SmartBatcher;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
class SmartBatcherTest {

    static {
        Log4jBridgeHandler.install(true, "", true);
    }

    @Test
    public void testSmartBatcher() throws InterruptedException {
        try (SmartBatcher<String> batcher = new SmartBatcher<>(this::handler, new ArrayBlockingQueue<>(1024), Executors.newSingleThreadExecutor()))
        {
            for (int i = 0; i < 1000000; i++) {
                batcher.submit("value"+i);
            }
            System.out.println("Close");
        }
        System.out.println("SmartBatcher closed");
        Assertions.assertEquals(1000000, count);
    }

    int count;

    private void handler(Collection<String> strings) {
        try {
            int size = strings.size();
            for (String string : strings) {
                count++;
                if (count%1000 == 0){
                    Thread.sleep(10);
                    System.out.println("Handled "+size+":"+count);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSmartBatcherException() throws InterruptedException{
        Assertions.assertThrows(IllegalStateException.class, ()->
        {
            try (SmartBatcher<String> batcher = new SmartBatcher<>(this::errorHandler, new ArrayBlockingQueue<>(1024), Executors.newSingleThreadExecutor()))
            {
                for (int i = 0; i < 1000000; i++) {
                    batcher.submit("value"+i);
                }
            }
        });
        System.out.println("Done");
    }

    private void errorHandler(Collection<String> strings) {
        int size = strings.size();
        for (String string : strings) {
            count++;
            if (count%1000 == 0){
                throw new IllegalArgumentException("Something went wrong");
            }
        }
    }

    @Test
    public void testCloseWhileHandlingLargeBatches()
            throws Exception
    {
        try (SmartBatcher<String> batcher = new SmartBatcher<>(this::slowHandler, new ArrayBlockingQueue<>(512), Executors.newSingleThreadExecutor()))
        {
            for (int i = 0; i < 2048; i++) {
                batcher.submit("value"+i);
            }
            System.out.println("Close");
        }
    }

    private void slowHandler(Collection<String> strings) {
        System.out.println("Starting");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Done");
    }
}