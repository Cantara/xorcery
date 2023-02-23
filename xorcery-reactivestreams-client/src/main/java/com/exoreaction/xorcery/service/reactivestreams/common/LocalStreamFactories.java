package com.exoreaction.xorcery.service.reactivestreams.common;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * This is used by the reactive stream client to get access to local stream factories, i.e. when authority is set to null.
 */
public interface LocalStreamFactories {

    WrappedSubscriberFactory getSubscriberFactory(String streamName);

    WrappedPublisherFactory getPublisherFactory(String streamName);

    static final class WrappedPublisherFactory {
        private final Function<Configuration, ? extends Flow.Publisher<Object>> factory;
        private final Class<? extends Flow.Publisher<?>> publisherType;

        public WrappedPublisherFactory(Function<Configuration, ? extends Flow.Publisher<Object>> factory,
                                       Class<? extends Flow.Publisher<?>> publisherType) {
            this.factory = factory;
            this.publisherType = publisherType;
        }

        public Function<Configuration, ? extends Flow.Publisher<Object>> factory() {
            return factory;
        }

        public Class<? extends Flow.Publisher<?>> publisherType() {
            return publisherType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (WrappedPublisherFactory) obj;
            return Objects.equals(this.factory, that.factory) &&
                   Objects.equals(this.publisherType, that.publisherType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(factory, publisherType);
        }

        @Override
        public String toString() {
            return "WrappedPublisherFactory[" +
                   "factory=" + factory + ", " +
                   "publisherType=" + publisherType + ']';
        }

        }

    static final class WrappedSubscriberFactory {
        private final Function<Configuration, Flow.Subscriber<Object>> factory;
        private final Class<? extends Flow.Subscriber<?>> subscriberType;

        public WrappedSubscriberFactory(Function<Configuration, Flow.Subscriber<Object>> factory,
                                        Class<? extends Flow.Subscriber<?>> subscriberType) {
            this.factory = factory;
            this.subscriberType = subscriberType;
        }

        public Function<Configuration, Flow.Subscriber<Object>> factory() {
            return factory;
        }

        public Class<? extends Flow.Subscriber<?>> subscriberType() {
            return subscriberType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (WrappedSubscriberFactory) obj;
            return Objects.equals(this.factory, that.factory) &&
                   Objects.equals(this.subscriberType, that.subscriberType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(factory, subscriberType);
        }

        @Override
        public String toString() {
            return "WrappedSubscriberFactory[" +
                   "factory=" + factory + ", " +
                   "subscriberType=" + subscriberType + ']';
        }

        }
}
