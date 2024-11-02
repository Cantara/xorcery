package dev.xorcery.reactivestreams.api;

import dev.xorcery.collections.Element;
import reactor.util.context.ContextView;

import java.util.Optional;
import java.util.function.Supplier;

public record ContextViewElement(ContextView context)
    implements Element
{
    public static Supplier<RuntimeException> missing(Object name) {
        return () -> new IllegalArgumentException("Missing context parameter '" + name + "'");
    }

    @Override
    public <T> Optional<T> get(String name) {
        return context.getOrEmpty(name);
    }

    @Override
    public <T> Optional<T> get(Enum<?> name) {
        return context.<T>getOrEmpty(name).or(()->context.getOrEmpty(name.name()));
    }

    @Override
    public boolean has(String name) {
        return context.hasKey(name);
    }

    @Override
    public boolean has(Enum<?> name) {
        return context.hasKey(name) || context.hasKey(name.name());
    }
}
