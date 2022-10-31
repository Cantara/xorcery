package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.TopicSubscribers;
import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.helpers.ClientPublisherGroupListener;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service
@Named(RequestLogService.SERVICE_TYPE)
public class RequestLogService
        implements PreDestroy {

    public static final String SERVICE_TYPE = "requestlog";
    private final Disruptor<WithMetadata<ObjectNode>> disruptor;

    private final ServiceResourceObject resourceObject;
    private final BroadcastEventHandler<WithMetadata<ObjectNode>> broadcastEventHandler = new BroadcastEventHandler<>(false);

    @Inject
    public RequestLogService(Topic<ServiceResourceObject> registryTopic,
                             ReactiveStreams reactiveStreams,
                             ServiceLocator serviceLocator,
                             Server server,
                             Configuration configuration) {
        this.resourceObject = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .build();

        disruptor = new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("RequestLogPublisher-"));
        disruptor.handleEventsWith(broadcastEventHandler);

        JsonRequestLog requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build(), disruptor.getRingBuffer());
        server.setRequestLog(requestLog);

        disruptor.start();

        resourceObject.getLinkByRel("requestlogevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new RequestLogPublisher(), RequestLogPublisher.class);
        });

        TopicSubscribers.addSubscriber(serviceLocator,new ClientPublisherGroupListener(resourceObject.getServiceIdentifier(), cfg -> new RequestLogPublisher(), RequestLogPublisher.class, null, reactiveStreams));

        registryTopic.publish(resourceObject);
    }


    @Override
    public void preDestroy() {
        disruptor.shutdown();
    }

    public class RequestLogPublisher
            implements Flow.Publisher<WithMetadata<ObjectNode>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
            subscriber.onSubscribe(broadcastEventHandler.add(subscriber));
        }
    }
}
