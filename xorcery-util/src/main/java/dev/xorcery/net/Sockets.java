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
package dev.xorcery.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

public interface Sockets {

    /**
     * Find free port to use in the range 49152-65535.
     * Use "{{ CALCULATED.dynamicPorts.myportname }}" in configurations instead of calling this manually.
     */
    static int nextFreePort() {
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
    static int nextFreePort(int from, int to) {
        int port = ThreadLocalRandom.current().nextInt(from, to);
        while (true) {
            if (isLocalPortFree(port)) {
                return port;
            } else {
                port = ThreadLocalRandom.current().nextInt(from, to);
            }
        }
    }

    static InetSocketAddress getInetSocketAddress(String hostPort, int defaultPort) {
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
}
