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
import java.util.function.Supplier;

/**
 * Wraps a Supplier into a lazy accessor. Great for caching expensive optional calculations.
 *
 * @param <T>
 */
public class LazySupplier<T>
        implements Supplier<T>, AutoCloseable
{
    private final Supplier<T> supplier;
    private final AtomicReference<T> value = new AtomicReference<>();

    private LazySupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Supplier<T> lazy(Supplier<T> supplier) {
        return new LazySupplier<>(supplier);
    }

    public T get() {
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
