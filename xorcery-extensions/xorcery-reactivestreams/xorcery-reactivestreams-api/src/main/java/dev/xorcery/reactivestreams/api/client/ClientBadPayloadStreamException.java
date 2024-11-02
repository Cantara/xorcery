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
package dev.xorcery.reactivestreams.api.client;

/**
 * Stream exception indicating that the client did something wrong, typically with configuration or authentication problems.
 */
public class ClientBadPayloadStreamException
    extends ClientStreamException
{
    public ClientBadPayloadStreamException(String message, Throwable cause) {
        super(1003, message, cause);
    }

    public ClientBadPayloadStreamException(String message) {
        super(1003, message);
    }
}
