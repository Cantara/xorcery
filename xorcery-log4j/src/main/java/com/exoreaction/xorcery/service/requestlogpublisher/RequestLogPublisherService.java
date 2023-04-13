package com.exoreaction.xorcery.service.requestlogpublisher;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.log4jpublisher.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service(name="requestlogpublisher")
public class RequestLogPublisherService
        implements PreDestroy {

    @Inject
    public RequestLogPublisherService(ReactiveStreamsClient reactiveStreams,
                                      Server server,
                                      Configuration configuration) {

        RequestLogConfiguration requestLogConfiguration = new RequestLogConfiguration(configuration.getConfiguration("requestlog"));
        RequestLogPublisher requestLogPublisher = new RequestLogPublisher();
        reactiveStreams.publish(requestLogConfiguration.getSubscriberAuthority(), requestLogConfiguration.getSubscriberStream(),
                requestLogConfiguration::getSubscriberConfiguration, requestLogPublisher, RequestLogPublisher.class, requestLogConfiguration.getPublisherConfiguration());

        JsonRequestLog requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(new InstanceConfiguration(configuration.getConfiguration("instance")))
                .build(), requestLogPublisher);
        server.setRequestLog(requestLog);
    }

    @Override
    public void preDestroy() {

    }

    public static class RequestLogPublisher
            implements Flow.Publisher<WithMetadata<ObjectNode>> {

        private Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber;

        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
            this.subscriber = subscriber;
        }

        public void send(WithMetadata<ObjectNode> event) {
            subscriber.onNext(event);
        }
    }
}
