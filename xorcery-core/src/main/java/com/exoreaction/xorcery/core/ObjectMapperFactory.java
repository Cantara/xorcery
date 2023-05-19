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
package com.exoreaction.xorcery.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

@Service
public class ObjectMapperFactory
    implements Factory<ObjectMapper>
{
    private final ObjectMapper objectMapper;

    public ObjectMapperFactory() {
        objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    @Rank(-1)
    @Singleton
    public ObjectMapper provide() {
        return objectMapper;
    }

    @Override
    public void dispose(ObjectMapper instance) {
    }
}
