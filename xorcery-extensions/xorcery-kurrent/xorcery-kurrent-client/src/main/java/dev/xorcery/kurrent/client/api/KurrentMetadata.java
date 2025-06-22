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
package dev.xorcery.kurrent.client.api;

/**
 * Metadata keys used by KurrentClient
 */
public enum KurrentMetadata {
    streamId, // String id of stream being read from
    streamPosition, // Long position of event in stream
    streamLive, // Boolean of whether the stream is now live (true on "caughtup" and false on "fellbehind")
    originalStreamId, // String id of stream an event was originally written to
    expectedPosition // Expected position of stream to write to (used for optimistic locking)
}
