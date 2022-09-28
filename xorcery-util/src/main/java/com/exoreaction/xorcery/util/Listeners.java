package com.exoreaction.xorcery.util;

import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper for delegating listener callbacks to a set of listeners.
 *
 * @param <T>
 */
public class Listeners<T> {
    private final T listener;
    private final List<T> listeners = new CopyOnWriteArrayList<>();

    public Listeners(Class<T> listenerInterface) {
        listener = listenerInterface.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{listenerInterface}, (proxy, method, args) ->
        {
            Object val = null;
            for (T l : listeners) {
                try {
                    val = method.invoke(l, args);
                } catch (Throwable e) {
                    LogManager.getLogger(getClass()).error("Could not invoke listener", e);
                }
            }
            return val;
        }));
    }

    public void addListener(T listener) {
        listeners.add(listener);
    }

    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    public T listener() {
        return listener;
    }
}
