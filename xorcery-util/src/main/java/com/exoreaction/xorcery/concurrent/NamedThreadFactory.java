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
package com.exoreaction.xorcery.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class NamedThreadFactory
    implements ThreadFactory
{
    private final String prefix;

    public NamedThreadFactory( String prefix )
    {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread( Runnable r )
    {
        Thread t = new Thread(r);
        t.setName( prefix+t.getId() );
        t.setDaemon(true);
        return t;
    }
}
