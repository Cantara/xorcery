package com.exoreaction.xorcery.util;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    public static InetSocketAddress getInetSocketAddress(String hostPort, int defaultPort)
    {
        String[] hostPortArray = hostPort.split(":");
        String host = hostPortArray[0];
        int port = hostPortArray.length == 2 ? Integer.parseInt(hostPortArray[1]) : defaultPort;
        return new InetSocketAddress(host, port);
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
