package com.exoreaction.xorcery.dns.server.udp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.server.DnsServerConfiguration;
import com.exoreaction.xorcery.dns.server.DnsServerService;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.xbill.DNS.Message.MAXLENGTH;

@Service(name = "dns.server.udp")
@RunLevel(4)
public class DNSUDPConnectorService
    implements PreDestroy
{
    private final DnsServerService dnsServerService;
    private final Logger logger;
    private final DatagramSocket socket;
    private ExecutorService executorService;
    private final byte[] in = new byte[MAXLENGTH];

    @Inject
    public DNSUDPConnectorService(Configuration configuration, DnsServerService dnsServerService, Logger logger) throws SocketException {
        this.dnsServerService = dnsServerService;
        this.logger = logger;

        DnsServerConfiguration dnsServerConfiguration = new DnsServerConfiguration(configuration.getConfiguration("dns.server"));

        executorService = Executors.newSingleThreadExecutor();
        socket = new DatagramSocket(dnsServerConfiguration.getPort());
        executorService.submit(this::process);
    }

    private void process() {
        try {
            // Read the request
            DatagramPacket indp = new DatagramPacket(in, MAXLENGTH);
            socket.receive(indp);

            Message request = new Message(in);
            Message response = new Message(request.getHeader().getID());
            response.addRecord(request.getQuestion(), Section.QUESTION);

            if (logger.isDebugEnabled())
                logger.debug("Request:" + request.toString());

            dnsServerService.handle(request, response);

            if (logger.isDebugEnabled())
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

    @Override
    public void preDestroy() {
        socket.close();
        executorService.shutdown();
        logger.info("Stopped DNS UDP Connector");
    }
}
