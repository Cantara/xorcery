package dev.xorcery.jgroups.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.jgroups.JChannelsService;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.jgroups.*;
import org.jgroups.util.MessageBatch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//@Service
//@RunLevel(20)
public class ChatBotServer implements  PreDestroy {
    private final InstanceConfiguration instanceConfiguration;
    private final Handler handler;

    private final JChannel channel;
    private final Logger logger;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Inject
    public ChatBotServer(JChannelsService jChannelsService, Configuration configuration, Logger logger) throws Exception {
        logger.info("Start chatbot", new Throwable());

        instanceConfiguration = InstanceConfiguration.get(configuration);
        this.logger = logger;
        handler = new Handler();
        channel = jChannelsService.newChannel("chat");
        channel.setName(instanceConfiguration.getId())
                .setDiscardOwnMessages(true)
                .setReceiver(handler)
                .connect("chat");
        scheduler.schedule(handler, 5, TimeUnit.SECONDS);
    }

    @Override
    public void preDestroy() {
        logger.info("Stop chatbot "+instanceConfiguration.getId());
        try {
            scheduler.submit(() -> scheduler.shutdown()).get();
        } catch (Throwable e) {
            // Ignore
        }
        channel.close();
    }

    private class Handler
            implements Receiver, Runnable
    {
        @Override
        public void receive(Message msg) {
            logger.info(instanceConfiguration.getId() + " received:" + msg.getObject());
        }

        @Override
        public void receive(MessageBatch batch) {
            logger.info(instanceConfiguration.getId() + " received batch:" + batch.size());
            for (Message message : batch) {
                receive(message);
            }
        }

        @Override
        public void viewAccepted(View new_view) {
            logger.info("View:" + new_view);
            if (channel.getAddress().equals(new_view.getCoord()))
            {
                logger.info("I am coordinator");
            }
        }

        int i = 0;

        @Override
        public void run() {

            try {
                channel.send(new ObjectMessage(null, "I am " + instanceConfiguration.getId() + ", message " + (++i)));
                scheduler.schedule(this, 5, TimeUnit.SECONDS);

            } catch (Exception e) {
                logger.error("Send failed", e);
            }
        }
    }
}
