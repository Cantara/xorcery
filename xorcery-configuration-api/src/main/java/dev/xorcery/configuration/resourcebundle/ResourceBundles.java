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
package dev.xorcery.configuration.resourcebundle;

import dev.xorcery.collections.Element;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public record ResourceBundles(ResourceBundle resourceBundle, String bundleName)
    implements Element
{
    public static ResourceBundles getBundle(String bundleName, Locale locale)
    {
        return new ResourceBundles(ResourceBundle.getBundle(ResourceBundles.class.getName(), locale), bundleName);
    }

    public static ResourceBundles getBundle(String bundleName)
    {
        return new ResourceBundles(ResourceBundle.getBundle(ResourceBundles.class.getName(), Locale.getDefault()), bundleName);
    }

    @Override
    public <T> Optional<T> get(String name) {
        try {
            return Optional.ofNullable((T)resourceBundle.getObject(bundleName+"."+name));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
