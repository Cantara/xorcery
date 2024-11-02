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