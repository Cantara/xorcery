package com.exoreaction.xorcery.service.jetty.server;


import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
@RunLevel(18)
public class JettyLifecycleService
        implements PreDestroy {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Server server;

    @Inject
    public JettyLifecycleService(Server server) throws Exception {
        this.server = server;
        server.start();
        logger.info("Started Jetty server");
    }

    @Override
    public void preDestroy() {
        logger.info("Stopping Jetty server");
        try {
            server.stop();
        } catch (Throwable e) {
            logger.error(e);
        }
    }
}
