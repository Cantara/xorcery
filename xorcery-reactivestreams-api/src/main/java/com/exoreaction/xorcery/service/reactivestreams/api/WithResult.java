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
package com.exoreaction.xorcery.service.reactivestreams.api;

import java.util.concurrent.CompletableFuture;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class WithResult<T,R>
{
    private T event;
    private CompletableFuture<R> result;

    public WithResult() {
    }

    public WithResult(T event, CompletableFuture<R> result) {
        this.event = event;
        this.result = result;
    }

    public void set(T event, CompletableFuture<R> result)
    {
        this.event = event;
        this.result = result;
    }

    public void set(WithResult<T,R> other)
    {
        this.event = other.event;
        this.result = other.result;
    }

    public T event()
    {
        return event;
    }

    public CompletableFuture<R> result()
    {
        return result;
    }
}
