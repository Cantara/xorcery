package dev.xorcery.reactivestreams.api;

import dev.xorcery.collections.Element;
import reactor.util.context.ContextView;

import java.util.Optional;

public record ContextViewElement(ContextView context)
    implements Element
{
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
