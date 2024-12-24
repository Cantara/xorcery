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
package dev.xorcery.jetty.server;

import dev.xorcery.util.UUIDs;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.HouseKeeper;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.sessions")
@Priority(2)
public class SessionHandlerFactory
    implements Factory<SessionHandler>
{

    private final SessionHandler sessionHandler;

    @Inject
    public SessionHandlerFactory(Server server) throws Exception {

        DefaultSessionIdManager idMgr = new DefaultSessionIdManager(server);
        idMgr.setWorkerName(UUIDs.newId());
        server.addBean(idMgr, true);

        HouseKeeper houseKeeper = new HouseKeeper();
        houseKeeper.setSessionIdManager(idMgr);
        houseKeeper.setIntervalSec(600L);
        idMgr.setSessionHouseKeeper(houseKeeper);

        sessionHandler = new SessionHandler();
        // TODO Add config options
    }

    @Override
    public SessionHandler provide() {
        return sessionHandler;
    }

    @Override
    public void dispose(SessionHandler instance) {

    }
}
