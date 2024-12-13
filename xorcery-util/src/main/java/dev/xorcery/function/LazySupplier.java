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
