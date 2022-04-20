package com.exoreaction.reactiveservices.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class NamedThreadFactory
    implements ThreadFactory
{
    private final String prefix;

    public NamedThreadFactory( String prefix )
    {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread( Runnable r )
    {
        Thread t = new Thread(r);
        t.setName( prefix+t.getId() );
        t.setDaemon(true);
        return t;
    }
}
