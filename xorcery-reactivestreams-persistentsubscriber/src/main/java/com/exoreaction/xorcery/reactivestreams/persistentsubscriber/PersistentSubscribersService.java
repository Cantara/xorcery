package com.exoreaction.xorcery.reactivestreams.persistentsubscriber;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import com.exoreaction.xorcery.process.ActiveProcesses;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

@Service(name = "persistentsubscribers")
@RunLevel(20)
public class PersistentSubscribersService
        implements PreDestroy {

    private final Logger logger;
    private final ActiveProcesses activeProcesses = new ActiveProcesses();

    @Inject
    public PersistentSubscribersService(
            Configuration configuration,
            Logger logger,
            LoggerContext loggerContext,
            ReactiveStreamsClient reactiveStreamsClient,
            IterableProvider<PersistentSubscriber> persistentSubscribers,
            IterableProvider<PersistentSubscriberCheckpoint> checkpointProviders,
            IterableProvider<PersistentSubscriberErrorLog> errorLogProviders) throws IOException {
        this.logger = logger;

        PersistentSubscribersConfiguration persistentSubscribersConfiguration = new PersistentSubscribersConfiguration(configuration.getConfiguration("persistentsubscribers"));

        logger.info("Subscribers:\n{}", String.join(",", StreamSupport.stream(persistentSubscribers.spliterator(), false).map(Object::toString).toList()));

        for (PersistentSubscriber persistentSubscriber : persistentSubscribers) {
            logger.info(persistentSubscriber);
        }

        for (PersistentSubscriberConfiguration subscriberConfig : persistentSubscribersConfiguration.getPersistentSubscribers()) {
            System.out.println(subscriberConfig.getName());

            PersistentSubscriber subscriber = persistentSubscribers.named(subscriberConfig.getName()).get();

            if (subscriber == null) {
                throw new IllegalArgumentException(String.format("Could not find persistent subscriber provider named '%s'", subscriberConfig.getName()));
            }

            String checkpointProviderName = subscriberConfig.getCheckpointProvider().orElseGet(persistentSubscribersConfiguration::getDefaultCheckpointProvider);
            PersistentSubscriberCheckpoint checkpoint = checkpointProviders.named(checkpointProviderName).get();
            if (checkpoint == null)
            {
                throw new IllegalArgumentException(String.format("Could not find checkpoint provider named '%s'", checkpointProviderName));
            }
            checkpoint.init(subscriberConfig);

            String errorLogProviderName = subscriberConfig.getErrorLogProvider().orElseGet(persistentSubscribersConfiguration::getDefaultErrorLogProvider);
            PersistentSubscriberErrorLog errorLog = errorLogProviders.named(errorLogProviderName).get();
            if (errorLog == null)
            {
                throw new IllegalArgumentException(String.format("Could not find error log provider named '%s'", errorLogProviderName));
            }
            errorLog.init(subscriberConfig, subscriber);

            CompletableFuture<Void> result = new CompletableFuture<>();
            activeProcesses.add(new PersistentSubscriberProcess(
                    subscriberConfig,
                    reactiveStreamsClient,
                    subscriber,
                    checkpoint,
                    errorLog,
                    loggerContext.getLogger(PersistentSubscriberProcess.class),
                    result
            )).start();
        }
    }

    @Override
    public void preDestroy() {
        try {
            activeProcesses.close();
        } catch (Exception e) {
            logger.error("Could not stop subscriber processes", e);
        }
    }
}
