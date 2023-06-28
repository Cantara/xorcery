/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.common;

import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.lang.Classes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public abstract class ReactiveStreamsAbstractService {
    // Magic bytes for sending exceptions
    public static final byte[] XOR = "XOR".getBytes(StandardCharsets.UTF_8);

    protected final MessageWorkers messageWorkers;
    protected final Logger logger;
    protected final ObjectMapper objectMapper;
    protected final ByteBufferPool byteBufferPool;

    //    protected final List<Flow.Subscription> activeSubscriptions = new CopyOnWriteArrayList<>();
    protected final List<SubscriberTracker> activeSubscribers = new CopyOnWriteArrayList<>();

    public ReactiveStreamsAbstractService(MessageWorkers messageWorkers, Logger logger) {
        this.messageWorkers = messageWorkers;
        this.logger = logger;
        this.objectMapper = new ObjectMapper();
        this.byteBufferPool = new ArrayByteBufferPool();
    }

    public void preDestroy() {
        logger.info("Stop reactive streams");

        cancelActiveSubscriptions();
    }

    protected void cancelActiveSubscriptions() {
        if (!activeSubscribers.isEmpty()) {
            logger.info("Cancel active subscriptions:" + activeSubscribers.size());

            // Cancel active subscriptions
            List<SubscriberTracker> currentSubscribers = new ArrayList<>(activeSubscribers);
            for (SubscriberTracker activeSubscriber : activeSubscribers) {
                activeSubscriber.getSubscription().cancel();
                activeSubscriber.onError(new ServerShutdownStreamException("Server is shutting down"));
            }

            // Wait for cleanup
            for (SubscriberTracker activeSubscriber : currentSubscribers) {
                // Wait for it to finish cleanly
                try {
                    activeSubscriber.getResult().get(10, TimeUnit.SECONDS);
                    logger.info("Subscriber cleaned up");
                } catch (Throwable e) {
                    // Ignore
                    logger.warn("Could not cancel subscription", e);
                }
            }
        }
    }

    protected Type getEventType(Type type) {
        return type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type;
    }

    protected Optional<Type> getResultType(Type type) {
        return Optional.ofNullable(type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[1] : null);
    }

    protected MessageWriter<Object> getWriter(Type type) {
        return Optional.ofNullable(messageWorkers.newWriter(Classes.getClass(type), type, "*/*"))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + type));
    }

    protected MessageReader<Object> getReader(Type type) {
        return Optional.ofNullable(messageWorkers.newReader(Classes.getClass(type), type, "*/*"))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + type));
    }


    /**
     * From https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection
     * <p>
     * Resolves the actual generic type arguments for a base class, as viewed from a subclass or implementation.
     *
     * @param <T>        base type
     * @param offspring  class or interface subclassing or extending the base type
     * @param base       base class
     * @param actualArgs the actual type arguments passed to the offspring class
     * @return actual generic type arguments, must match the type parameters of the offspring class. If omitted, the
     * type parameters will be used instead.
     */
    public static <T> Type[] resolveActualTypeArgs(Class<? extends T> offspring, Class<T> base, Type... actualArgs) {

        assert offspring != null;
        assert base != null;
        assert actualArgs.length == 0 || actualArgs.length == offspring.getTypeParameters().length;

        //  If actual types are omitted, the type parameters will be used instead.
        if (actualArgs.length == 0) {
            actualArgs = offspring.getTypeParameters();
        }
        if (actualArgs.length == 0 && offspring.getTypeParameters().length > 0) {
            actualArgs = ((ParameterizedType) offspring.getGenericSuperclass()).getActualTypeArguments();
        }
        // map type parameters into the actual types
        Map<String, Type> typeVariables = new HashMap<String, Type>();
        for (int i = 0; i < actualArgs.length; i++) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) offspring.getTypeParameters()[i];
            typeVariables.put(typeVariable.getName(), actualArgs[i]);
        }

        // Find direct ancestors (superclass, interfaces)
        List<Type> ancestors = new LinkedList<Type>();
        if (offspring.getGenericSuperclass() != null) {
            ancestors.add(offspring.getGenericSuperclass());
        }
        for (Type t : offspring.getGenericInterfaces()) {
            ancestors.add(t);
        }

        // Recurse into ancestors (superclass, interfaces)
        for (Type type : ancestors) {
            if (type instanceof Class<?>) {
                // ancestor is non-parameterized. Recurse only if it matches the base class.
                Class<?> ancestorClass = (Class<?>) type;
                if (base.isAssignableFrom(ancestorClass)) {
                    Type[] result = resolveActualTypeArgs((Class<? extends T>) ancestorClass, base);
                    if (result != null) {
                        return result;
                    }
                }
            }
            if (type instanceof ParameterizedType) {
                // ancestor is parameterized. Recurse only if the raw type matches the base class.
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?>) {
                    Class<?> rawTypeClass = (Class<?>) rawType;
                    if (base.isAssignableFrom(rawTypeClass)) {

                        // loop through all type arguments and replace type variables with the actually known types
                        List<Type> resolvedTypes = new LinkedList<Type>();
                        for (Type t : parameterizedType.getActualTypeArguments()) {
                            if (t instanceof TypeVariable<?>) {
                                Type resolvedType = typeVariables.get(((TypeVariable<?>) t).getName());
                                resolvedTypes.add(resolvedType != null ? resolvedType : t);
                            } else {
                                resolvedTypes.add(t);
                            }
                        }

                        Type[] result = resolveActualTypeArgs((Class<? extends T>) rawTypeClass, base, resolvedTypes.toArray(new Type[]{}));
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }

        // we have a result if we reached the base class.
        return offspring.equals(base) ? actualArgs : null;
    }

    public class SubscriberTracker implements Flow.Subscriber<Object> {
        private final Flow.Subscriber<Object> subscriber;
        private final CompletableFuture<Void> result;
        private Flow.Subscription subscription;

        public SubscriberTracker(Flow.Subscriber<Object> subscriber, CompletableFuture<Void> result) {
            this.subscriber = subscriber;
            this.result = result;
            activeSubscribers.add(this);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(Object item) {
            subscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            if (activeSubscribers.remove(this)) {
                subscriber.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (activeSubscribers.remove(this)) {
                subscriber.onComplete();
            }
        }

        public Flow.Subscription getSubscription() {
            return subscription;
        }

        public CompletableFuture<Void> getResult() {
            return result;
        }
    }

    public class PublisherTracker implements Flow.Publisher<Object> {
        private final Flow.Publisher<Object> publisher;

        public PublisherTracker(Flow.Publisher<Object> publisher) {
            this.publisher = publisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Object> subscriber) {
            publisher.subscribe(new SubscriberTracker(subscriber, new CompletableFuture<>()));
        }
    }
}
