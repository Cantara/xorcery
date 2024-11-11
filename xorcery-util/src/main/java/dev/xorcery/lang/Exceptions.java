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

import java.util.Optional;

public interface Exceptions {

    static Throwable unwrap(Throwable throwable)
    {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    @SafeVarargs
    static boolean isCausedBy(Throwable throwable, Class<? extends Throwable>... exceptionClasses)
    {
        do
        {
            for (Class<? extends Throwable> exceptionClass : exceptionClasses) {
                if (exceptionClass.isInstance(throwable))
                    return true;
            }
            throwable = throwable.getCause();
        } while (throwable != null);
        return false;
    }

    static <T extends Throwable> Optional<T> getCause(Throwable throwable, Class<T> exceptionClass)
    {
        do {
            if (exceptionClass.isInstance(throwable))
            {
                //noinspection unchecked
                return Optional.of((T)throwable);
            }
        } while ((throwable = throwable.getCause()) != null);

        return Optional.empty();
    }
}
