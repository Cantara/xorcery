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
package com.exoreaction.xorcery.log4jpublisher.log4j;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.xorcery.log4jpublisher.LoggingMetadataEventHandler;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
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

@Plugin(name = "Log4jPublisher", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4jPublisherAppender
        extends AbstractAppender {

    public static class Builder<B extends Log4jPublisherAppender.Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<Log4jPublisherAppender> {
        @Override
        public Log4jPublisherAppender build() {
            final Layout<? extends Serializable> layout = getOrCreateLayout();

            return new Log4jPublisherAppender(getName(), layout, getFilter(), isIgnoreExceptions(), getPropertyArray());
        }
    }

    @PluginBuilderFactory
    public static <B extends Log4jPublisherAppender.Builder<B>> B newBuilder() {
        return new Log4jPublisherAppender.Builder<B>().asBuilder();
    }

    private final Disruptor<WithMetadata<LogEvent>> disruptor;
    private final LoggingMetadataEventHandler loggingMetadataEventHandler = new LoggingMetadataEventHandler();
    private final UnicastEventHandler<WithMetadata<LogEvent>> unicastEventHandler = new UnicastEventHandler<>();

    public Log4jPublisherAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
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

        if (event.getContextData().containsKey("log4jsubscriber"))
        {
            return;
        }

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
