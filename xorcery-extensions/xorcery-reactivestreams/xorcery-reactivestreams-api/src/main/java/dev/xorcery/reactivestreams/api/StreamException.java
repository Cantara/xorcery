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

/**
 * All exceptions reported to subscribers will be wrapped in a subclass of this exception.
 */
public class StreamException
    extends RuntimeException
{
    private final int status;

    public StreamException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public StreamException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
