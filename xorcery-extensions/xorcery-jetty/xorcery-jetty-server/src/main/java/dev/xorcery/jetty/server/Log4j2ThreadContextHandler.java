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
package dev.xorcery.jetty.server;

import dev.xorcery.configuration.ApplicationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import org.apache.logging.log4j.ThreadContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.HashMap;
import java.util.Map;

public class Log4j2ThreadContextHandler
    extends Handler.Wrapper
{
    private final Map<String, String> context;

    public Log4j2ThreadContextHandler(Configuration configuration) {
        context = new HashMap<>();
        InstanceConfiguration instanceConfiguration = InstanceConfiguration.get(configuration);
        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.get(configuration);
        context.put("id", instanceConfiguration.getId());
        context.put("name", applicationConfiguration.getName());
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        ThreadContext.putAll(context);
        try {
            return super.handle(request, response, callback);
        } finally {
            ThreadContext.clearAll();
        }
    }
}
