package com.exoreaction.xorcery.coordinator.test;

import com.exoreaction.xorcery.coordinator.Main;
import com.exoreaction.xorcery.core.Xorcery;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.concurrent.CompletableFuture;

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
