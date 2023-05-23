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
package com.exoreaction.xorcery.handlebars.helpers;

import com.github.jknack.handlebars.ValueResolver;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Support for Optional values
 */
public class OptionalValueResolver
    implements ValueResolver
{
    public static final ValueResolver INSTANCE = new OptionalValueResolver();

    @Override
    public Object resolve(Object context, String name) {
        return ValueResolver.UNRESOLVED;
    }

    @Override
    public Object resolve(Object context) {
        if (context instanceof Optional optional)
        {
            return optional.orElse(null);
        } else
        {
            return ValueResolver.UNRESOLVED;
        }
    }

    @Override
    public Set<Map.Entry<String, Object>> propertySet(Object context) {
        return Collections.emptySet();
    }
}
