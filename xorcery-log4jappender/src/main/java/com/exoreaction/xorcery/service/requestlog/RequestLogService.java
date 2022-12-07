package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service(name="requestlog")
public class RequestLogService
        implements PreDestroy {

    @Inject
    public RequestLogService(ReactiveStreamsClient reactiveStreams,
                             Server server,
                             Configuration configuration) {

        RequestLogPublisher requestLogPublisher = new RequestLogPublisher();
        reactiveStreams.publish(configuration.getString("requestlog.host").orElseThrow(), configuration.getString("requestlog.stream").orElseThrow(),
                () -> configuration.getConfiguration("requestlog.configuration"), requestLogPublisher, RequestLogPublisher.class, Configuration.empty());

        JsonRequestLog requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
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
