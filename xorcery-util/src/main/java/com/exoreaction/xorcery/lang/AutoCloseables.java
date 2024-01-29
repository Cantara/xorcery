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
package com.exoreaction.xorcery.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Collect a list of AutoCloseable implementations so you can close them in one go. Exceptions thrown on close() are
 * collected using the "suppressed exception" mechanism.
 */
public class AutoCloseables
    implements AutoCloseable
{
    private final List<AutoCloseable> closeables = new ArrayList<>();


    public <T extends AutoCloseable> T add(T closeable)
    {
        closeables.add(closeable);
        return closeable;
    }

    @Override
    public void close() throws Exception {

        Exception exception = null;
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
                else
                {
                    e.addSuppressed(exception);
                    exception = e;
                }
            }
        }
        if (exception != null)
            throw exception;
    }
}
