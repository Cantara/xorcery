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
package com.exoreaction.xorcery.eventstore.resources;

import com.exoreaction.xorcery.jsonschema.server.annotations.AttributeSchema;

public class EventStoreParameters
{
        @AttributeSchema(title="Stream name", description = "Name of the streamId to subscribe to", required = true)
        public String stream;
        @AttributeSchema(title="From streamPosition", description = "Position to subscribe from. If omitted then read from beginning of streamId", required = false)
        public long from;
}
