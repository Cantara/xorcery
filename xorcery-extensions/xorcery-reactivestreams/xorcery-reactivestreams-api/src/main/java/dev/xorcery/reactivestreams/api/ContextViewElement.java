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
