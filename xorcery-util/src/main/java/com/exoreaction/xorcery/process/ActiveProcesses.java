package com.exoreaction.xorcery.process;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper to track active processes and close them on shutdown.
 */
public class ActiveProcesses
        implements AutoCloseable {
    List<Process<?>> processes = new CopyOnWriteArrayList<>();

    <T extends Process<?>> T add(T process) {
        processes.add(process);
        process.result().whenComplete((v,t)->processes.remove(process));
        return process;
    }

    @Override
    public void close() throws Exception {
        for (Process<?> process : processes) {
            process.stop();
        }
    }
}
