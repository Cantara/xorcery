package com.exoreaction.xorcery.jetty.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.util.UUIDs;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.HouseKeeper;
import org.eclipse.jetty.session.SessionHandler;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service
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
    @Named("jetty.server.sessions")
    @Singleton
    public SessionHandler provide() {
        return sessionHandler;
    }

    @Override
    public void dispose(SessionHandler instance) {

    }
}
