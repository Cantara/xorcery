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
package dev.xorcery.jetty.client;


import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Result;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureResponseListener extends org.eclipse.jetty.client.BufferingResponseListener {
    private CompletableFuture<ContentResponse> future;

    public CompletableFutureResponseListener(CompletableFuture<ContentResponse> future, int maxLength) {
        super(maxLength);
        this.future = future;
    }

    public CompletableFutureResponseListener(CompletableFuture<ContentResponse> future) {
        this(future, 2 * 1024 * 1024);
    }

    @Override
    public void onComplete(Result result) {
        if (result.isFailed()) {
            future.completeExceptionally(result.getFailure());
        } else {
            future.complete(ContentResponse.from(result.getResponse(), getContent(), getMediaType(), getEncoding()));
        }
    }
}
