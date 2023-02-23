package com.exoreaction.xorcery.function;

import java.util.concurrent.Callable;
import java.util.function.Function;

public interface Factory<P,T>
    extends Function<P, Callable<T>>
{
}
