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
package com.exoreaction.xorcery.dns.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.xbill.DNS.Message.MAXLENGTH;
import static org.xbill.DNS.Record.fromString;

@Service(name = "dns.server")
@RunLevel(2)
public class DnsServerService
        implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());

    static final int FLAG_DNSSECOK = 1;
    static final int FLAG_SIGONLY = 2;

    private final DatagramSocket socket;
    private final byte[] in = new byte[MAXLENGTH];

    private ExecutorService executorService;
    private Configuration configuration;
    private final int port;

    private final Map<String, TSIG> TSIGs = new HashMap<>();
    private final Map<Name, TSIG> zoneTSIGs = new HashMap<>();
    private final Map<Name, Zone> znames = new ConcurrentHashMap<>();
    private final Map<Integer, Cache> caches = new ConcurrentHashMap<>();

    @Inject
    public DnsServerService(Configuration configuration) throws IOException {
        this.configuration = configuration;
        InstanceConfiguration standardConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        DnsServerConfiguration dnsServerConfiguration = new DnsServerConfiguration(configuration.getConfiguration("dns.server"));
        port = dnsServerConfiguration.getPort();

        // Create keys
        dnsServerConfiguration.getKeys().ifPresent(list -> list.forEach(kc ->
        {
            TSIGs.put(kc.getName(), new TSIG(kc.getAlgorithm(), kc.getName(), kc.getSecret()));
        }));

        // Create Zones
        dnsServerConfiguration.getZones().ifPresent(list -> list.forEach(zc ->
        {
            try {
                Name origin = Name.fromString(zc.getName(), Name.root);
                Name nameServer = Name.fromString("ns1", origin);
                Zone zone = new Zone(origin, new Record[]{new SOARecord(
                        origin,
                        DClass.IN,
                        60,
                        nameServer,
                        nameServer,
                        1, 600, 600, 600, 86400),
                        new NSRecord(origin, DClass.IN, 600, Name.fromString(standardConfiguration.getIp().getHostAddress(), Name.root))});
                znames.put(zone.getOrigin(), zone);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));

        executorService = Executors.newSingleThreadExecutor();
        socket = new DatagramSocket(port);
        executorService.submit(this::process);

        logger.info("Started DNS server");
    }

    @Override
    public void preDestroy() {
        executorService.shutdown();
        socket.close();
    }

    private void process() {
        try {
            // Read the request
            DatagramPacket indp = new DatagramPacket(in, MAXLENGTH);
            socket.receive(indp);

            Message request = new Message(in);
            Message response = new Message(request.getHeader().getID());
            response.addRecord(request.getQuestion(), Section.QUESTION);

            logger.debug("Request:" + request.toString());

            handle(request, response);

            logger.debug("Response:" + response.toString());

            byte[] resp = response.toWire();
            DatagramPacket outdp = new DatagramPacket(resp, resp.length, indp.getAddress(), indp.getPort());
            socket.send(outdp);
        } catch (SocketException e) {
            if (e.getCause() instanceof AsynchronousCloseException) {
                // Done
                return;
            } else {
                logger.error("Failed to handle DNS request", e);
            }
        } catch (Throwable e) {
            logger.error("Failed to handle DNS request", e);
        }

        executorService.submit(this::process);
    }

    private void handle(Message request, Message response)
            throws IOException {
        Header header = request.getHeader();

        if (header.getRcode() != Rcode.NOERROR) {
            errorMessage(request.getHeader(), response, Rcode.FORMERR, request.getQuestion());
            return;
        }

        switch (header.getOpcode()) {
            case Opcode.QUERY -> queryRequest(request, response);
            case Opcode.UPDATE -> updateRequest(request, response);
            default -> errorMessage(request.getHeader(), response, Rcode.NOTIMP, request.getQuestion());
        }
    }

    private void queryRequest(Message request, Message response) {
        Header header = request.getHeader();
        int flags = 0;

        if (header.getOpcode() != Opcode.QUERY) {
            errorMessage(request.getHeader(), response, Rcode.NOTIMP, request.getQuestion());
            return;
        }

        Record question = request.getQuestion();

        response.getHeader().setFlag(Flags.QR);
        if (header.getFlag(Flags.RD)) {
            response.getHeader().setFlag(Flags.RD);
        }

/* NYI
        TSIGRecord queryTSIG = request.getTSIG();
        TSIG tsig = null;
        if (queryTSIG != null) {
            tsig = TSIGs.get(queryTSIG.getName());
            if (tsig == null || tsig.verify(query, in, null) != Rcode.NOERROR) {
                return formerrMessage(in);
            }
        }
*/

        if (!Type.isRR(question.getType()) && question.getType() != Type.ANY) {
            errorMessage(header, response, Rcode.NOTIMP, question);
            return;
        }

        int rcode = addAnswer(response, question.getName(), question.getType(), question.getDClass(), 0, 0);

        if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
            errorMessage(header, response, rcode, question);
            return;
        }

        addAdditional(response, flags);

    }

    private void updateRequest(Message request, Message response) {

        // TODO Signature check

        Name zoneName = request.getQuestion().getName();
        Zone zone = znames.get(zoneName);
        if (zone == null) {
            errorMessage(request.getHeader(), response, Rcode.NXDOMAIN, request.getQuestion());
            return;
        }

        for (Record record : request.getSection(Section.UPDATE)) {
            if (record.getDClass() == DClass.NONE) {
                Record originalRecord = Record.newRecord(record.getName(), record.getType(), DClass.IN, 0, record.rdataToWireCanonical());
                zone.removeRecord(originalRecord);
            } else {
                zone.addRecord(record);
            }
        }
    }

    private int addAnswer(Message response, Name name, int type, int dClass, int iterations, int flags) {
        logger.info("Answer for:" + name);

        SetResponse sr;
        int rcode = Rcode.NOERROR;

        if (iterations > 6) {
            return Rcode.NOERROR;
        }

        if (type == Type.SIG || type == Type.RRSIG) {
            type = Type.ANY;
            flags |= FLAG_SIGONLY;
        }

        Zone zone = findBestZone(name);
        if (zone != null) {
            sr = zone.findRecords(name, type);
        } else {
            Cache cache = getCache(dClass);
            sr = cache.lookupRecords(name, type, Credibility.NORMAL);
        }

/*
        if (sr.isUnknown()) {
            addCacheNS(response, getCache(dClass), name);
        }
*/
        if (sr.isUnknown() || sr.isNXDOMAIN()) {
            response.getHeader().setRcode(Rcode.NXDOMAIN);
            if (zone != null) {
                addSOA(response, zone);
                if (iterations == 0) {
                    response.getHeader().setFlag(Flags.AA);
                }
            }
            rcode = Rcode.NXDOMAIN;
        } else if (sr.isNXRRSET()) {
            if (zone != null) {
                addSOA(response, zone);
                if (iterations == 0) {
                    response.getHeader().setFlag(Flags.AA);
                }
            }
        } else if (sr.isDelegation()) {
            RRset nsRecords = sr.getNS();
            addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
        } else if (sr.isCNAME()) {
            CNAMERecord cname = sr.getCNAME();
            RRset rrset = new RRset(cname);
            addRRset(name, response, rrset, Section.ANSWER, flags);
            if (zone != null && iterations == 0) {
                response.getHeader().setFlag(Flags.AA);
            }
            rcode = addAnswer(response, cname.getTarget(), type, dClass, iterations + 1, flags);
        } else if (sr.isDNAME()) {
            DNAMERecord dname = sr.getDNAME();
            RRset rrset = new RRset(dname);
            addRRset(name, response, rrset, Section.ANSWER, flags);
            Name newname;
            try {
                newname = name.fromDNAME(dname);
            } catch (NameTooLongException e) {
                return Rcode.YXDOMAIN;
            }

            CNAMERecord cname = new CNAMERecord(name, dClass, 0, newname);
            RRset cnamerrset = new RRset(cname);
            addRRset(name, response, cnamerrset, Section.ANSWER, flags);
            if (zone != null && iterations == 0) {
                response.getHeader().setFlag(Flags.AA);
            }
            rcode = addAnswer(response, newname, type, dClass, iterations + 1, flags);
        } else if (sr.isSuccessful()) {
            List<RRset> rrsets = sr.answers();
            for (RRset rrset : rrsets) {
                addRRset(name, response, rrset, Section.ANSWER, flags);
            }
            if (zone != null) {
                addNS(response, zone, flags);
                if (iterations == 0) {
                    response.getHeader().setFlag(Flags.AA);
                }
            } else {
                addCacheNS(response, getCache(dClass), name);
            }
        }
        return rcode;
    }

    public Zone findBestZone(Name name) {
        Zone foundzone;
        foundzone = znames.get(name);
        if (foundzone != null) {
            return foundzone;
        }
        int labels = name.labels();
        for (int i = 1; i < labels; i++) {
            Name tname = new Name(name, i);
            foundzone = znames.get(tname);
            if (foundzone != null) {
                return foundzone;
            }
        }
        return null;
    }

    public Cache getCache(int dclass) {
        return caches.computeIfAbsent(dclass, Cache::new);
    }

    public RRset findExactMatch(Name name, int type, int dclass, boolean glue) {
        Zone zone = findBestZone(name);
        if (zone != null) {
            return zone.findExactMatch(name, type);
        } else {
            List<RRset> rrsets;
            Cache cache = getCache(dclass);
            if (glue) {
                rrsets = cache.findAnyRecords(name, type);
            } else {
                rrsets = cache.findRecords(name, type);
            }
            if (rrsets == null) {
                return null;
            } else {
                return rrsets.get(0); /* not quite right */
            }
        }
    }

    void addRRset(Name name, Message response, RRset rrset, int section, int flags) {
        for (int s = 1; s <= section; s++) {
            if (response.findRRset(name, rrset.getType(), s)) {
                return;
            }
        }
        if ((flags & FLAG_SIGONLY) == 0) {
            for (Record r : rrset.rrs()) {
                if (r.getName().isWild() && !name.isWild()) {
                    r = r.withName(name);
                }
                response.addRecord(r, section);
            }
        }
        if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
            for (Record r : rrset.sigs()) {
                if (r.getName().isWild() && !name.isWild()) {
                    r = r.withName(name);
                }
                response.addRecord(r, section);
            }
        }
    }

    private void addSOA(Message response, Zone zone) {
        response.addRecord(zone.getSOA(), Section.AUTHORITY);
    }

    private void addNS(Message response, Zone zone, int flags) {
        RRset nsRecords = zone.getNS();
        addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY, flags);
    }

    private void addCacheNS(Message response, Cache cache, Name name) {
        SetResponse sr = cache.lookupRecords(name, Type.NS, Credibility.HINT);
        if (!sr.isDelegation()) {
            return;
        }
        RRset nsRecords = sr.getNS();
        for (Record r : nsRecords.rrs()) {
            response.addRecord(r, Section.AUTHORITY);
        }
    }

    private void addGlue(Message response, Name name, int flags) {
        RRset a = findExactMatch(name, Type.A, DClass.IN, true);
        if (a == null) {
            return;
        }
        addRRset(name, response, a, Section.ADDITIONAL, flags);
    }

    private void addAdditional2(Message response, int section, int flags) {
        for (Record r : response.getSection(section)) {
            Name glueName = r.getAdditionalName();
            if (glueName != null) {
                addGlue(response, glueName, flags);
            }
        }
    }

    private void addAdditional(Message response, int flags) {
        addAdditional2(response, Section.ANSWER, flags);
        addAdditional2(response, Section.AUTHORITY, flags);
    }

    private int addAnswerOld(Message response, Name name, int type, int dClass, int iterations, int flags) {
        // TODO This needs to be more like jnamed
        // Add answers as needed
        switch (type) {
            case Type.A: {
                return configuration.getJson("dns.hosts." + name.toString(true)).map(result ->
                {
                    if (result.isTextual()) {
                        try {
                            response.addRecord(fromString(name, Type.A, DClass.IN, 86400, result.textValue(), Name.root), Section.ANSWER);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else if (result instanceof ArrayNode array) {
                        for (JsonNode jsonNode : array) {
                            try {
                                response.addRecord(fromString(name, Type.A, DClass.IN, 86400, jsonNode.textValue(), Name.root), Section.ANSWER);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                    return Rcode.NOERROR;
                }).orElse(Rcode.NXDOMAIN);
            }

            case Type.SRV: {
                return configuration.getJson("dns.hosts." + name.toString(true)).map(result ->
                {
                    if (result.isTextual()) {
                        try {
                            response.addRecord(toSrvRecord(name, 60, 1, 1, result.textValue()), Section.ANSWER);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else if (result instanceof ArrayNode array) {
                        for (JsonNode jsonNode : array) {
                            try {
                                response.addRecord(toSrvRecord(name, 60, 1, 1, jsonNode.textValue()), Section.ANSWER);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                    return Rcode.NOERROR;
                }).orElse(Rcode.NXDOMAIN);
            }

            default: {
            }
        }
        return Rcode.NXDOMAIN;
    }

    private void errorMessage(Header header, Message response, int rcode, Record question) {
        for (int i = 0; i < 4; i++) {
            response.removeAllRecords(i);
        }
//        if (rcode == Rcode.SERVFAIL) {
        response.addRecord(question, Section.QUESTION);
//        }
        header.setRcode(rcode);
    }

    private SRVRecord toSrvRecord(Name name, int ttl, int priority, int weight, String target) throws TextParseException {
        String[] hostPort = target.split(":");
        String host = hostPort[0] + ".";
        int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1]) : -1;
        return new SRVRecord(name, Type.SRV, ttl, priority, weight, port, Name.fromString(host));
    }

}
