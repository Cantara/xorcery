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
package dev.xorcery.function;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps a AtomicReference into a lazy accessor. Great for caching expensive optional calculations.
 * This variation allows you to create the instance in one place and define the lambda somewhere else.
 * <pre>{@code
 *     LazyReference<String> someValue = lazyReference();
 *     ...
 *     public String getSomeValue() {
 *       return someValue.apply(()->"This is an expensive value");
 *     }
 *
 * }</pre>
 *
 * @param <T>
 */
public class LazyReference<T>
        implements Function<Supplier<T>, T>, AutoCloseable {
    private final AtomicReference<T> value = new AtomicReference<>();

    private LazyReference() {
    }

    public static <T> LazyReference<T> lazyReference() {
        return new LazyReference<>();
    }

    @Override
    public T apply(Supplier<T> supplier) {
        return value.updateAndGet(v ->
        {
            if (v != null)
                return v;
            return supplier.get();
        });
    }

    @Override
    public void close() throws Exception {
        try {
            if (value.get() instanceof AutoCloseable closeable) {
                closeable.close();
            }
        } finally {
            value.set(null);
        }
    }
}
