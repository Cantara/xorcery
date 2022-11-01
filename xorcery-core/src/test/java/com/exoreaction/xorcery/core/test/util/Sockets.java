package com.exoreaction.xorcery.core.test.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

public final class Sockets {

    /**
     * Find free port to use in the range 49152-65535.
     *
     */
    public static int nextFreePort() {
        return nextFreePort(49152, 65535);
    }

    /**
     * Find free port to use in a range.
     * Mainly for test configurations running in CI.
     *
     * @param from
     * @param to
     * @return
     */
    public static int nextFreePort(int from, int to) {
        int port = ThreadLocalRandom.current().nextInt(from, to);
        while (true) {
            if (isLocalPortFree(port)) {
                return port;
            } else {
                port = ThreadLocalRandom.current().nextInt(from, to);
            }
        }
    }

    private static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Sockets() {
    }
}
