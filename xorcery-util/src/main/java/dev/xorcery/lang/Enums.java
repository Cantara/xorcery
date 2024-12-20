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
package dev.xorcery.lang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Enums {

    Map<Enum<?>, String> fieldMappings = new ConcurrentHashMap<>();

    /**
     * Map an enum Some.name to "some_name"
     *
     * @param anEnum enum value
     * @return stringified enum
     */
    public static String toField(Enum<?> anEnum) {
        return fieldMappings.computeIfAbsent(anEnum, e ->
                e.getDeclaringClass().getSimpleName().toLowerCase() + "_" + e.name());
    }
}
