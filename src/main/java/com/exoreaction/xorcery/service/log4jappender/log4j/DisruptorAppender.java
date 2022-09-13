package com.exoreaction.xorcery.service.log4jappender.log4j;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadataEventHandler;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Plugin(name = "Disruptor", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class DisruptorAppender
        extends AbstractAppender {

    public static class Builder<B extends DisruptorAppender.Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<DisruptorAppender> {
        @Override
        public DisruptorAppender build() {
            final Layout<? extends Serializable> layout = getOrCreateLayout();

            return new DisruptorAppender(getName(), layout, getFilter(), isIgnoreExceptions(), getPropertyArray());
        }
    }

    @PluginBuilderFactory
    public static <B extends DisruptorAppender.Builder<B>> B newBuilder() {
        return new DisruptorAppender.Builder<B>().asBuilder();
    }

    private final Disruptor<WithMetadata<LogEvent>> disruptor;
    private final LoggingMetadataEventHandler loggingMetadataEventHandler = new LoggingMetadataEventHandler();
    private final UnicastEventHandler<WithMetadata<LogEvent>> unicastEventHandler = new UnicastEventHandler<>();

    public DisruptorAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                             final boolean ignoreExceptions, final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        Objects.requireNonNull(layout, "layout");

        disruptor = new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("Log4jDisruptor-"),
                ProducerType.MULTI,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(loggingMetadataEventHandler)
                .then(unicastEventHandler);
    }

    @Override
    public void append(final LogEvent event) {

        disruptor.publishEvent((holder, seq, e) ->
        {
            Metadata.Builder builder = new Metadata.Builder();
            builder.add("timestamp", System.currentTimeMillis());
            ThreadContext.getContext().forEach(builder::add);
            holder.set(builder.build(), e);
        }, event);
    }

    @Override
    public void start() {
        disruptor.start();
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {

        try {
            disruptor.shutdown(timeout, timeUnit);
        } catch (TimeoutException e) {
            // Ignore
        }
        return super.stop(timeout, timeUnit);
    }

    public UnicastEventHandler<WithMetadata<LogEvent>> getUnicastEventHandler() {
        return unicastEventHandler;
    }

    public void setConfiguration(Configuration configuration) {
        loggingMetadataEventHandler.configuration.set(configuration);
    }

    @Override
    public String toString() {
        return "DisruptorAppender{" +
                "name=" + getName() +
                ", state=" + getState() +
                '}';
    }
}
