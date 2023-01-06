package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.util.Classes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.glassfish.hk2.api.PreDestroy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ReactiveStreamsAbstractService
    implements PreDestroy
{
    // Magic bytes for sending exceptions
    public static final byte[] XOR = "XOR".getBytes(StandardCharsets.UTF_8);

    protected final Logger logger = LogManager.getLogger(getClass());

    protected final MessageWorkers messageWorkers;
    protected final ObjectMapper objectMapper;
    protected final ByteBufferPool byteBufferPool;
    protected final ScheduledExecutorService timer;

    protected final List<Flow.Subscription> activeSubscriptions = new CopyOnWriteArrayList<>();

    public ReactiveStreamsAbstractService(MessageWorkers messageWorkers) {
        this.messageWorkers = messageWorkers;
        this.objectMapper = new ObjectMapper();
        this.byteBufferPool = new ArrayByteBufferPool();
        timer = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void preDestroy() {
        logger.info("Stop reactive streams");

        timer.shutdown();

        logger.info("Cancel active subscriptions:" + activeSubscriptions.size());

        // Cancel active subscriptions
        for (Flow.Subscription activeSubscription : activeSubscriptions) {
            activeSubscription.cancel();
            // TODO Populate corresponding CompletableFuture
        }

    }

    protected Type getEventType(Type type) {
        return type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type;
    }

    protected Optional<Type> getResultType(Type type) {
        return Optional.ofNullable(type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[1] : null);
    }

    protected MessageWriter<Object> getWriter(Type type) {
        return Optional.ofNullable(messageWorkers.newWriter(Classes.getClass(type), type, MediaType.WILDCARD_TYPE.toString()))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + type));
    }

    protected MessageReader<Object> getReader(Type type) {
        return Optional.ofNullable(messageWorkers.newReader(Classes.getClass(type), type, MediaType.WILDCARD_TYPE.toString()))
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

    protected class SubscriberTracker implements Flow.Subscriber<Object> {
        private final Flow.Subscriber<Object> subscriber;
        private Flow.Subscription subscription;

        public SubscriberTracker(Flow.Subscriber<Object> subscriber) {
            this.subscriber = subscriber;
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
            activeSubscriptions.remove(subscription);
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            activeSubscriptions.remove(subscription);
            subscriber.onComplete();
        }
    }

    public class PublisherTracker implements Flow.Publisher<Object> {
        private final Flow.Publisher<Object> publisher;

        public PublisherTracker(Flow.Publisher<Object> publisher) {
            this.publisher = publisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Object> subscriber) {
            publisher.subscribe(new SubscriberTracker(subscriber));
        }
    }
}
