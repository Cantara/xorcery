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
package com.exoreaction.xorcery.secrets.spi;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public interface SecretsProvider {

    default String getSecretString(String name)
            throws UncheckedIOException, IllegalArgumentException
    {
        return new String(getSecretBytes(name), StandardCharsets.UTF_8);
    }

    default byte[] getSecretBytes(String name)
            throws UncheckedIOException, IllegalArgumentException
    {
        return getSecretString(name).getBytes(StandardCharsets.UTF_8);
    }

    default void refreshSecret(String name)
            throws UncheckedIOException, IllegalArgumentException
    {
    }
}
