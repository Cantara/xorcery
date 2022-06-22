package com.exoreaction.xorcery.service.handlebars.helpers;

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
        return UNRESOLVED;
    }

    @Override
    public Object resolve(Object context) {
        if (context instanceof Optional optional)
        {
            return optional.orElse(null);
        } else
        {
            return UNRESOLVED;
        }
    }

    @Override
    public Set<Map.Entry<String, Object>> propertySet(Object context) {
        return Collections.emptySet();
    }
}
