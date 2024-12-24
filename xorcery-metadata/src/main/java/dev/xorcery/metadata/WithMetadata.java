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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

public abstract class WithMetadata<T>
{
    private Metadata metadata;
    private T data;

    public WithMetadata() {
    }

    @JsonCreator(mode = PROPERTIES)
    public WithMetadata(@JsonProperty("metadata") Metadata metadata, @JsonProperty("data") T data) {
        this.metadata = metadata;
        this.data = data;
    }

    public void set(Metadata metadata, T data)
    {
        this.metadata = metadata;
        this.data = data;
    }

    public void set(WithMetadata<T> other)
    {
        this.metadata = other.metadata;
        this.data = other.data;
    }

    @JsonGetter
    public Metadata metadata()
    {
        return metadata;
    }

    @JsonGetter
    public T data()
    {
        return data;
    }
}
