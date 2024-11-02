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
package dev.xorcery.dns.client.providers.test;

import dev.xorcery.dns.client.providers.SRVLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerEntryTest {

    @Test
    public void testServerEntryPrioritySorting() {
        SRVLookup.ServerEntry server1 = new SRVLookup.ServerEntry(URI.create("http://localhost:80"), 1, 0, 1);
        SRVLookup.ServerEntry server2 = new SRVLookup.ServerEntry(URI.create("http://localhost:81"), 2, 0, 1);
        SRVLookup.ServerEntry server3 = new SRVLookup.ServerEntry(URI.create("http://localhost:82"), 3, 0, 1);

        List<SRVLookup.ServerEntry> servers = new ArrayList<>(List.of(server1, server2, server3));
        Collections.sort(servers);
        Assertions.assertEquals(List.of(server1, server2, server3), servers);

    }

    @Test
    public void testServerEntryPriorityWeightSorting() {
        SRVLookup.ServerEntry server1 = new SRVLookup.ServerEntry(URI.create("http://localhost:80"), 1, 1, 1);
        SRVLookup.ServerEntry server2 = new SRVLookup.ServerEntry(URI.create("http://localhost:81"), 2, 1, 5);
        SRVLookup.ServerEntry server3 = new SRVLookup.ServerEntry(URI.create("http://localhost:82"), 2, 1, 10);
        SRVLookup.ServerEntry server4 = new SRVLookup.ServerEntry(URI.create("http://localhost:83"), 3, 1, 1);

        List<SRVLookup.ServerEntry> servers = new ArrayList<>(List.of(server1, server2, server3, server4));
        Collections.sort(servers);
        Assertions.assertEquals(List.of(server1, server3, server2, server4), servers);
    }

    @Test
    public void testWeightWithRequestsSorting() {
        URI uri1 = URI.create("http://localhost:80");
        URI uri2 = URI.create("http://localhost:81");
        URI uri3 = URI.create("http://localhost:82");
        Map<URI, AtomicInteger> requests = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            SRVLookup.ServerEntry server1 = new SRVLookup.ServerEntry(uri1, 1, requests.computeIfAbsent(uri1, u -> new AtomicInteger()).get(), 10);
            SRVLookup.ServerEntry server2 = new SRVLookup.ServerEntry(uri2, 1, requests.computeIfAbsent(uri2, u -> new AtomicInteger()).get(), 40);
            SRVLookup.ServerEntry server3 = new SRVLookup.ServerEntry(uri3, 1, requests.computeIfAbsent(uri3, u -> new AtomicInteger()).get(), 50);
            List<SRVLookup.ServerEntry> servers = new ArrayList<>(List.of(server1, server2, server3));
            Collections.sort(servers);
            requests.get(servers.get(0).uri()).incrementAndGet();
        }

        System.out.println(requests);
    }
}
