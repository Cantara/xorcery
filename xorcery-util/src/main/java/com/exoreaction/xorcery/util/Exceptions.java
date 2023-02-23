package com.exoreaction.xorcery.util;

public interface Exceptions {

    static Throwable unwrap(Throwable throwable)
    {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }
}
