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
package dev.xorcery.metadata;

import dev.xorcery.builders.WithContext;

import java.util.Optional;

public interface CommonMetadata
    extends WithContext<Metadata>
{

    interface Builder<T>
    {
        Metadata.Builder builder();

        default T timestamp(long value) {
            builder().add("timestamp", value);
            return (T)this;
        }

        default T contentType(String value) {
            builder().add("contentType", value);
            return (T)this;
        }
    }

    default long getTimestamp() {
        return context().getLong("timestamp").orElse(0L);
    }

    default Optional<String> getContentType()
    {
        return context().getString("contentType");
    }
}
