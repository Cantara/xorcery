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
package dev.xorcery.dns.server.tcp;

import dev.xorcery.dns.server.DnsServerService;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.IteratingCallback;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.WireParseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

public class DNSTCPConnection extends AbstractConnection {
    private final IteratingCallback callback = new DNSIteratingCallback();
    private final DnsServerService dnsServerService;
    private final Logger logger;

    public DNSTCPConnection(EndPoint endPoint, Executor executor, DnsServerService dnsServerService, Logger logger) {
        super(endPoint, executor);
        this.dnsServerService = dnsServerService;
        this.logger = logger;
        logger.debug("DNS connection from {}", endPoint.getRemoteSocketAddress().toString());
    }

    @Override
    public void onOpen() {
        super.onOpen();

        // Declare interest in being called back when
        // there are bytes to read from the network.
        fillInterested();
    }

    @Override
    public void onFillable() {
        callback.iterate();
    }

    private class DNSIteratingCallback extends IteratingCallback {
        private ByteBuffer buffer;

        @Override
        protected Action process() throws Throwable {
            if (buffer == null)
                buffer = BufferUtil.allocate(Message.MAXLENGTH, true);
            int length = -1;
            while (true) {
                int filled = getEndPoint().fill(buffer);

                if (buffer.limit() >= 2 && length == -1) {
                    length = buffer.getShort();
                }

                if (filled > 0) {
                    if (buffer.limit() == length + 2) {
                        try {
                            // TODO Convert this into an async handling
                            Message request = new Message(buffer);
                            Message response = new Message(request.getHeader().getID());
                            response.addRecord(request.getQuestion(), Section.QUESTION);

                            if (logger.isDebugEnabled())
                                logger.debug("Request:" + request.toString());
                            dnsServerService.handle(request, response);
                            if (logger.isDebugEnabled())
                                logger.debug("Response:" + response.toString());
                            byte[] resp = response.toWire();

                            // Prefix with length
                            byte[] bytes = new byte[2 + resp.length];
                            bytes[0] = (byte) (resp.length >>> 8);
                            bytes[1] = (byte) (resp.length & 0xFF);
                            System.arraycopy(resp, 0, bytes, 2, resp.length);

                            getEndPoint().flush(ByteBuffer.wrap(bytes));
                            fillInterested();

                            return Action.IDLE;
                        } catch (WireParseException e) {
                            // Continue
                        } catch (IOException e) {
                            onCompleteFailure(e);
                        }
                    }
                } else if (filled == 0) {
                    // We don't need the buffer anymore, so
                    // don't keep it around while we are idle.
                    buffer = null;

                    // No more bytes to read, declare
                    // again interest for fill events.
                    fillInterested();

                    // Signal that the iteration is now IDLE.
                    return Action.IDLE;
                } else {
                    // The other peer closed the connection,
                    // the iteration completed successfully.
                    return Action.SUCCEEDED;
                }
            }
        }

        @Override
        protected void onCompleteSuccess() {
            logger.debug("onCompleteSuccess");
            getEndPoint().close();
        }

        @Override
        protected void onCompleteFailure(Throwable cause) {
            logger.debug("onCompleteFailure", cause);
            getEndPoint().close(cause);
        }
    }
}