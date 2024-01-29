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
package com.exoreaction.xorcery.neo4jprojections.streams;

/**
 * A preprocessor can throw this to indicate that the current batch should be released, and the event reprocessed.
 *
 * One cornercase is to allow an event processor to be ensured that any database transaction is done against a projection
 * which has all previous events written to it already.
 */
public class EarlyReleaseException
    extends RuntimeException
{
    public static final EarlyReleaseException INSTANCE = new EarlyReleaseException();
}
