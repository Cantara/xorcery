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
