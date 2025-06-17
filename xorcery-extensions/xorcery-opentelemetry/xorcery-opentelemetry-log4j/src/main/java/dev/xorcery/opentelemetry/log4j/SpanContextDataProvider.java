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
package dev.xorcery.opentelemetry.log4j;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.apache.logging.log4j.core.util.ContextDataProvider;

import java.util.HashMap;
import java.util.Map;

public class SpanContextDataProvider
        implements ContextDataProvider
{
    @Override
    public Map<String, String> supplyContextData() {
        Map<String, String> map = new HashMap<>();
        if (Span.current() instanceof ReadableSpan readableSpan){
            readableSpan.getAttributes().forEach((key, value)-> map.put(key.getKey(), value.toString()));
        }
        return map;
    }
}
