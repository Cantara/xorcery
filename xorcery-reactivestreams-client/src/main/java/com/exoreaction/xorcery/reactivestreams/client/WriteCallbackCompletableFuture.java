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
package com.exoreaction.xorcery.reactivestreams.client;

import com.exoreaction.xorcery.builders.With;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.concurrent.CompletableFuture;

public record WriteCallbackCompletableFuture(CompletableFuture<Void> future)
    implements WriteCallback, With<WriteCallbackCompletableFuture>
{
    public WriteCallbackCompletableFuture() {
        this(new CompletableFuture<>());
    }

    @Override
    public void writeFailed(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void writeSuccess() {
        future.complete(null);
    }
}
