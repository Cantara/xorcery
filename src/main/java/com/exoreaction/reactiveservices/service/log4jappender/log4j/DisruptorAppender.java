package com.exoreaction.reactiveservices.service.log4jappender.log4j;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.handlers.MetadataSerializerEventHandler;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
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
import org.apache.logging.log4j.core.layout.JsonLayout;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Plugin( name = "Disruptor", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true )
public class DisruptorAppender
    extends AbstractAppender
{
    public static class Builder<B extends DisruptorAppender.Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<DisruptorAppender>
    {
        @Override
        public DisruptorAppender build()
        {
            final Layout<? extends Serializable> layout = getOrCreateLayout();

            return new DisruptorAppender( getName(), layout, getFilter(), isIgnoreExceptions(), getPropertyArray() );
        }
    }

    @PluginBuilderFactory
    public static <B extends DisruptorAppender.Builder<B>> B newBuilder()
    {
        return new DisruptorAppender.Builder<B>().asBuilder();
    }

    private final Disruptor<Event<LogEvent>> disruptor;
    private final List<EventHandler<Event<LogEvent>>> consumers = new CopyOnWriteArrayList<>();

    public DisruptorAppender( final String name, final Layout<? extends Serializable> layout, final Filter filter,
                              final boolean ignoreExceptions, final Property[] properties )
    {
        super( name, filter, layout, ignoreExceptions, properties );
        Objects.requireNonNull( layout, "layout" );

        disruptor =
            new Disruptor<>( Event::new, 4096, new NamedThreadFactory( "Log4jDisruptor-" ),
                ProducerType.MULTI,
                new BlockingWaitStrategy() );

        disruptor.handleEventsWith(
                     new MetadataSerializerEventHandler(),
                     new Log4jSerializeEventHandler( JsonLayout.newBuilder().build() ) )
                 .then( new BroadcastEventHandler<>( consumers ) );
    }

    @Override
    public void start()
    {
        disruptor.start();
        super.start();
    }

    @Override
    public void append( final LogEvent event )
    {
        disruptor.publishEvent( ( holder, seq, e ) ->
        {
            holder.metadata.clear();
            holder.metadata.getMetadata().putAll( ThreadContext.getContext() );
            holder.metadata.add("appname", "Test");
            holder.event = e;
        }, event );
    }

    @Override
    public boolean stop( final long timeout, final TimeUnit timeUnit )
    {
        setStopping();
        boolean stopped = super.stop( timeout, timeUnit, false );
        setStopped();
        return stopped;
    }

    public List<EventHandler<Event<LogEvent>>> getConsumers()
    {
        return consumers;
    }

    @Override
    public String toString()
    {
        return "DisruptorAppender{" +
               "name=" + getName() +
               ", state=" + getState() +
               '}';
    }
}
