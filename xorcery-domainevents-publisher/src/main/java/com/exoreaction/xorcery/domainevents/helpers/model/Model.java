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
package com.exoreaction.xorcery.domainevents.helpers.model;

import com.exoreaction.xorcery.json.model.JsonElement;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface Model
    extends JsonElement
{
    Map<Enum<?>,String> fieldMappings = new ConcurrentHashMap<>();

    default Optional<String> getString(Enum<?> name) {
        return getString(toField(name));
    }

    default Optional<Integer> getInteger(Enum<?> name) {
        return getInteger(toField(name));
    }

    default Optional<Long> getLong(Enum<?> name) {
        return getLong(toField(name));
    }

    default Optional<Boolean> getBoolean(Enum<?> name) {
        return getBoolean(toField(name));
    }

    default <T extends Enum<T>> Optional<T> getEnum(Enum<?> name, Class<T> enumClass) {
        return getEnum(toField(name), enumClass);
    }

    default String toField( Enum<?> anEnum )
    {
        return fieldMappings.computeIfAbsent( anEnum, e ->
                e.getDeclaringClass().getSimpleName().toLowerCase() + "_" + e.name() );
    }
}
