package com.exoreaction.xorcery.service.reactivestreams.api;

import com.lmax.disruptor.*;

public interface SubscriptionEventSink<T>
    extends EventSink<WithMetadata<T>>,AutoCloseable
{
    public record Impl<T>(EventSink<WithMetadata<T>> eventSink, AutoCloseable closeable)
        implements SubscriptionEventSink<T>
    {
        @Override
        public void publishEvent(EventTranslator<WithMetadata<T>> translator) {
            eventSink.publishEvent(translator);
        }

        @Override
        public boolean tryPublishEvent(EventTranslator<WithMetadata<T>> translator) {
            return eventSink.tryPublishEvent(translator);
        }

        @Override
        public <A> void publishEvent(EventTranslatorOneArg<WithMetadata<T>,A> translator, A arg0) {
            eventSink.publishEvent(translator, arg0);
        }

        @Override
        public <A> boolean tryPublishEvent(EventTranslatorOneArg<WithMetadata<T>,A> translator, A arg0) {
            return eventSink.tryPublishEvent(translator, arg0);
        }

        @Override
        public <A,B> void publishEvent(EventTranslatorTwoArg<WithMetadata<T>,A, B> translator, A arg0, B arg1) {
            eventSink.publishEvent(translator, arg0, arg1);

        }

        @Override
        public <A,B> boolean tryPublishEvent(EventTranslatorTwoArg<WithMetadata<T>,A,B> translator, A arg0, B arg1) {
            return eventSink.tryPublishEvent(translator, arg0, arg1);
        }

        @Override
        public <A,B,C> void publishEvent(EventTranslatorThreeArg<WithMetadata<T>,A,B,C> translator, A arg0, B arg1, C arg2) {
            eventSink.publishEvent(translator, arg0, arg1, arg2);

        }

        @Override
        public <A,B,C> boolean tryPublishEvent(EventTranslatorThreeArg<WithMetadata<T>,A,B,C> translator, A arg0, B arg1, C arg2) {
            return eventSink.tryPublishEvent(translator, arg0, arg1, arg2);
        }

        @Override
        public void publishEvent(EventTranslatorVararg<WithMetadata<T>> translator, Object... args) {
            eventSink.publishEvent(translator, args);

        }

        @Override
        public boolean tryPublishEvent(EventTranslatorVararg<WithMetadata<T>> translator, Object... args) {
            return eventSink.tryPublishEvent(translator, args);
        }

        @Override
        public void publishEvents(EventTranslator<WithMetadata<T>>[] translators) {
            eventSink.publishEvents(translators);

        }

        @Override
        public void publishEvents(EventTranslator<WithMetadata<T>>[] translators, int batchStartsAt, int batchSize) {
            eventSink.publishEvents(translators, batchStartsAt, batchSize);

        }

        @Override
        public boolean tryPublishEvents(EventTranslator<WithMetadata<T>>[] translators) {
            return eventSink.tryPublishEvents(translators);
        }

        @Override
        public boolean tryPublishEvents(EventTranslator<WithMetadata<T>>[] translators, int batchStartsAt, int batchSize) {
            return eventSink.tryPublishEvents(translators, batchStartsAt, batchSize);
        }

        @Override
        public <A> void publishEvents(EventTranslatorOneArg<WithMetadata<T>,A> translator, A[] arg0) {
            eventSink.publishEvents(translator, arg0);

        }

        @Override
        public <A> void publishEvents(EventTranslatorOneArg<WithMetadata<T>, A> translator, int batchStartsAt, int batchSize, A[] arg0) {
            eventSink.publishEvents(translator, batchStartsAt, batchSize, arg0);

        }

        @Override
        public <A> boolean tryPublishEvents(EventTranslatorOneArg<WithMetadata<T>, A> translator, A[] arg0) {
            return eventSink.tryPublishEvents(translator, arg0);
        }

        @Override
        public <A> boolean tryPublishEvents(EventTranslatorOneArg<WithMetadata<T>, A> translator, int batchStartsAt, int batchSize, A[] arg0) {
            return eventSink.tryPublishEvents(translator, batchStartsAt, batchSize, arg0);
        }

        @Override
        public <A,B> void publishEvents(EventTranslatorTwoArg<WithMetadata<T>, A, B> translator, A[] arg0, B[] arg1) {
            eventSink.publishEvents(translator, arg0, arg1);

        }

        @Override
        public <A,B> void publishEvents(EventTranslatorTwoArg<WithMetadata<T>, A, B> translator, int batchStartsAt, int batchSize, A[] arg0, B[] arg1) {
            eventSink.publishEvents(translator, batchStartsAt, batchSize, arg0, arg1);
        }

        @Override
        public <A,B> boolean tryPublishEvents(EventTranslatorTwoArg<WithMetadata<T>, A, B> translator, A[] arg0, B[] arg1) {
            return eventSink.tryPublishEvents(translator, arg0, arg1);
        }

        @Override
        public <A,B> boolean tryPublishEvents(EventTranslatorTwoArg<WithMetadata<T>, A, B> translator, int batchStartsAt, int batchSize, A[] arg0, B[] arg1) {
            return eventSink.tryPublishEvents(translator, batchStartsAt, batchSize, arg0, arg1);
        }

        @Override
        public <A,B,C> void publishEvents(EventTranslatorThreeArg<WithMetadata<T>,A,B,C> translator, A[] arg0, B[] arg1, C[] arg2) {
            eventSink.publishEvents(translator, arg0, arg1, arg2);

        }

        @Override
        public <A,B,C> void publishEvents(EventTranslatorThreeArg<WithMetadata<T>,A,B,C> translator, int batchStartsAt, int batchSize, A[] arg0, B[] arg1, C[] arg2) {
            eventSink.publishEvents(translator, batchStartsAt, batchSize, arg0, arg1, arg2);

        }

        @Override
        public <A,B,C> boolean tryPublishEvents(EventTranslatorThreeArg<WithMetadata<T>,A,B,C> translator, A[] arg0, B[] arg1, C[] arg2) {
            return eventSink.tryPublishEvents(translator, arg0, arg1, arg2);
        }

        @Override
        public <A,B,C> boolean tryPublishEvents(EventTranslatorThreeArg<WithMetadata<T>,A,B,C> translator, int batchStartsAt, int batchSize, A[] arg0, B[] arg1, C[] arg2) {
            return eventSink.tryPublishEvents(translator, batchStartsAt, batchSize, arg0, arg1, arg2);
        }

        @Override
        public void publishEvents(EventTranslatorVararg<WithMetadata<T>> translator, Object[]... args) {
            eventSink.publishEvents(translator, args);

        }

        @Override
        public void publishEvents(EventTranslatorVararg<WithMetadata<T>> translator, int batchStartsAt, int batchSize, Object[]... args) {
            eventSink.publishEvents(translator, batchStartsAt, batchSize, args);

        }

        @Override
        public boolean tryPublishEvents(EventTranslatorVararg<WithMetadata<T>> translator, Object[]... args) {
            return eventSink.tryPublishEvents(translator, args);
        }

        @Override
        public boolean tryPublishEvents(EventTranslatorVararg<WithMetadata<T>> translator, int batchStartsAt, int batchSize, Object[]... args) {
            return eventSink.tryPublishEvents(translator, batchStartsAt, batchSize, args);
        }

        @Override
        public void close() throws Exception {
            closeable.close();
        }
    }
}
