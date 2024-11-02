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
package dev.xorcery.builders;

import java.util.function.Consumer;

/**
 * Builders can implement this to make it easy to use visitor pattern. This avoids breaking the DSL flow use in many cases.
 *
 * @param <T>
 */
public interface With<T> {

    @SuppressWarnings("unchecked")
    default T with( Consumer<T>... consumers )
    {
        for ( Consumer<T> consumer : consumers )
        {
            consumer.accept( (T)this );
        }
        return (T)this;
    }
}
