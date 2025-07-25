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
package dev.xorcery.collections;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This represents a map of values which can be accessed through these helper methods.
 * They do null and type checks and conversions (if necessary) so that client code can focus
 * on what they want to assert about the values rather than messing with what is actually in the map.
 */
public interface Element {

    static Supplier<RuntimeException> missing(Object name) {
        return () -> new IllegalArgumentException("Missing '" + name + "'");
    }

    static Element empty()
    {
        return new Element() {
            @Override
            public <T> Optional<T> get(String name) {
                return Optional.empty();
            }
        };
    }

    <T> Optional<T> get(String name);

    default <T> Optional<T> get(Enum<?> name)
    {
        return get(name.name());
    }

    default boolean has(String name) {
        return get(name).isPresent();
    }

    default boolean has(Enum<?> name) {
        return get(name).isPresent();
    }

    default Optional<String> getString(String name) {
        return get(name).map(Object::toString);
    }

    default Optional<String> getString(Enum<?> name) {
        return get(name).map(Object::toString);
    }

    default <T extends Enum<T>> Optional<T> getEnum(String name, Class<T> enumClass) {
        Optional<String> value = getString(name);

        if (value.isPresent()) {
            try {
                return Optional.of(Enum.valueOf(enumClass, value.get()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    default <T extends Enum<T>> Optional<T> getEnum(Enum<?> name, Class<T> enumClass) {
        Optional<String> value = getString(name);

        if (value.isPresent()) {
            try {
                return Optional.of(Enum.valueOf(enumClass, value.get()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    default Optional<URI> getURI(String name) {

        return getString(name).map(s -> s.replace('\\', '/').replace(' ', '+')).map(URI::create);
    }

    default Optional<URI> getURI(Enum<?> name) {
        return getString(name).map(s -> s.replace('\\', '/').replace(' ', '+')).map(URI::create);
    }

    default Optional<URL> getURL(String name) {

        return getString(name).map(s -> s.replace('\\', '/').replace(' ', '+')).map(url -> {
            try {
                return new URL(url);
            } catch (MalformedURLException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }

    default Optional<URL> getURL(Enum<?> name) {
        return getURL(name.name());
    }

    default Optional<Short> getShort(String name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.shortValue());
            } else {
                try {
                    return Optional.of(Short.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Short> getShort(Enum<?> name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.shortValue());
            } else {
                try {
                    return Optional.of(Short.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Integer> getInteger(String name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.intValue());
            } else {
                try {
                    return Optional.of(Integer.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Integer> getInteger(Enum<?> name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.intValue());
            } else {
                try {
                    return Optional.of(Integer.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Long> getLong(String name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.longValue());
            } else {
                try {
                    return Optional.of(Long.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Long> getLong(Enum<?> name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.longValue());
            } else {
                try {
                    return Optional.of(Long.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Float> getFloat(String name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.floatValue());
            } else {
                try {
                    return Optional.of(Float.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Float> getFloat(Enum<?> name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.floatValue());
            } else {
                try {
                    return Optional.of(Float.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Double> getDouble(String name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.doubleValue());
            } else {
                try {
                    return Optional.of(Double.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Double> getDouble(Enum<?> name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Number i) {
                return Optional.of(i.doubleValue());
            } else {
                try {
                    return Optional.of(Double.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Boolean> getBoolean(String name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Boolean bool) {
                return Optional.of(bool);
            } else {
                try {
                    return Optional.of(Boolean.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Optional<Boolean> getBoolean(Enum<?> name) {
        return get(name).flatMap(value ->
        {
            if (value instanceof Boolean bool) {
                return Optional.of(bool);
            } else {
                try {
                    return Optional.of(Boolean.valueOf(value.toString()));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        });
    }

    default Boolean getFalsy(String name) {
        return get(name).map(value ->
        {
            if (value instanceof Boolean bool)
                return bool;
            else if (value instanceof String str)
                return !str.isBlank();
            else if (value instanceof Number nr)
                return nr.longValue() != 0;
            else
                return false;
        }).orElse(false);
    }

    default Boolean getFalsy(Enum<?> name) {
        return get(name).map(value ->
        {
            if (value instanceof Boolean bool)
                return bool;
            else if (value instanceof String str)
                return !str.isBlank();
            else if (value instanceof Number nr)
                return nr.longValue() != 0;
            else
                return false;
        }).orElse(false);
    }

    default <T,R> Optional<T> getMapped(String name, Function<R, T> wrapper){
        return this.<R>get(name).map(wrapper);
    }
}
