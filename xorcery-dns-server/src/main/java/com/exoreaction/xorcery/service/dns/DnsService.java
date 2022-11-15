package com.exoreaction.xorcery.service.dns;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.xbill.DNS.Record.fromString;

@Service(name = "dns.server")
public class DnsService
        implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());

    private static final int UDP_SIZE = 512;
    private final DatagramSocket socket;

    private ExecutorService executorService;
    private Configuration configuration;
    private final int port;

    private final Map<Name, TSIG> TSIGs = new HashMap<>();


    @Inject
    public DnsService(Configuration configuration) throws IOException {
        this.configuration = configuration;
        port = configuration.getInteger("dns.server.port").orElse(53);

        executorService = Executors.newSingleThreadExecutor();
        socket = new DatagramSocket(port);
        executorService.submit(this::process);
    }

    @Override
    public void preDestroy() {
        executorService.shutdown();
        socket.close();
    }

    private void process() {
        try {
            byte[] in = new byte[UDP_SIZE];
            // Read the request
            DatagramPacket indp = new DatagramPacket(in, UDP_SIZE);
            socket.receive(indp);

            Message request = new Message(in);
            Message response = new Message(request.getHeader().getID());
            response.addRecord(request.getQuestion(), Section.QUESTION);

            handle(request, response);
            byte[] resp = response.toWire();
            DatagramPacket outdp = new DatagramPacket(resp, resp.length, indp.getAddress(), indp.getPort());
            socket.send(outdp);
        } catch (SocketException e) {
            if (!(e.getCause() instanceof AsynchronousCloseException)) {
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
    }

    private int addAnswer(Message response, Name name, int type, int dClass, int iterations, int flags) {
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
        if (rcode == Rcode.SERVFAIL) {
            response.addRecord(question, Section.QUESTION);
        }
        header.setRcode(rcode);
    }

    private SRVRecord toSrvRecord(Name name, int ttl, int priority, int weight, String target) throws TextParseException {
        String[] hostPort = target.split(":");
        String host = hostPort[0] + ".";
        int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1]) : -1;
        return new SRVRecord(name, Type.SRV, ttl, priority, weight, port, Name.fromString(host));
    }
}
