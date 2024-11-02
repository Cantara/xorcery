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
package dev.xorcery.dns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.xbill.DNS.Record.newRecord;
import static org.xbill.DNS.Section.ANSWER;

@Disabled
public class DnsTest {

    @Test
    public void testDnsA() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        Record queryRecord = newRecord(Name.fromString("web.example.com."), Type.A, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        Resolver r = new SimpleResolver("127.0.0.1");
        r.sendAsync(queryMessage)
                .whenComplete(
                        (answer, ex) -> {
                            if (ex == null) {
                                System.out.println(answer);
                            } else {
                                ex.printStackTrace();
                            }
                        })
                .toCompletableFuture()
                .get();
    }

    @Test
    public void testDnsSRV() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        Record queryRecord = newRecord(Name.fromString("_web._tcp.example.com."), Type.SRV, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        Resolver r = new SimpleResolver("127.0.0.1");
        r.sendAsync(queryMessage)
                .whenComplete(
                        (answer, ex) -> {
                            if (ex == null) {
                                System.out.println(answer);
                            } else {
                                ex.printStackTrace();
                            }
                        })
                .toCompletableFuture()
                .get();
    }

    @Test
    public void testDnsTXT() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        Record queryRecord = newRecord(Name.fromString("_web._tcp.example.com."), Type.TXT, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        Resolver r = new SimpleResolver("127.0.0.1");
        r.sendAsync(queryMessage)
                .whenComplete(
                        (answer, ex) -> {
                            if (ex == null) {
                                System.out.println(answer);
                            } else {
                                ex.printStackTrace();
                            }
                        })
                .toCompletableFuture()
                .get();
    }

    @Test
    public void testServiceLookup() throws IOException {
        Resolver resolver = new SimpleResolver("127.0.0.1");

        Record queryRecord = newRecord(Name.fromString("_analytics._tcp.example.com."), Type.SRV, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        Message responseMessage = resolver.send(queryMessage);

        List<InetSocketAddress> servers = new ArrayList<>();
        for (Record record : responseMessage.getSection(ANSWER)) {
            SRVRecord srvRecord = (SRVRecord) record;
            String host = responseMessage.getSection(Section.ADDITIONAL).stream().filter(r -> r.getName().equals(srvRecord.getAdditionalName())).findFirst().orElse(null).rdataToString();
            servers.add(new InetSocketAddress(host, srvRecord.getPort()));
        }
        System.out.println(servers);

    }
}
