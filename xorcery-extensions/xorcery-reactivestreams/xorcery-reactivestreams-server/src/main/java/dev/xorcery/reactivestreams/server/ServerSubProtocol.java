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
package dev.xorcery.reactivestreams.server;

import io.opentelemetry.context.Context;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;

import java.util.Map;

public interface ServerSubProtocol
    extends AutoCloseable
{
    void close();

    Session.Listener.AutoDemanding createSubProtocolHandler(
            ServerUpgradeRequest serverUpgradeRequest,
            ServerUpgradeResponse serverUpgradeResponse,
            String clientHost,
            String path,
            Map<String, String> pathParameters,
            int clientMaxBinaryMessageSize,
            Context context);
}
