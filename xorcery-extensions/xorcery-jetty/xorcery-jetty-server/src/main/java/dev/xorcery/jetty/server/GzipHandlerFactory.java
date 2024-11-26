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

import dev.xorcery.configuration.Configuration;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.gzip")
@Priority(10)
public class GzipHandlerFactory
        implements Factory<GzipHandler> {
    private final GzipHandler gzipHandler;

    @Inject
    public GzipHandlerFactory(Configuration configuration) {
        gzipHandler = new GzipHandler();
        GzipConfiguration gzipConfiguration = GzipConfiguration.get(configuration);
        gzipConfiguration.getExcludedMediaTypes().forEach(gzipHandler::addExcludedMimeTypes);
        gzipConfiguration.getExcludedMethods().forEach(gzipHandler::addExcludedMethods);
        gzipConfiguration.getExcludedPaths().forEach(gzipHandler::addExcludedPaths);
        gzipConfiguration.getIncludedMediaTypes().forEach(gzipHandler::addIncludedMimeTypes);
        gzipConfiguration.getIncludedMethods().forEach(gzipHandler::addIncludedMethods);
        gzipConfiguration.getIncludedPaths().forEach(gzipHandler::addIncludedPaths);
        gzipConfiguration.getMinGzipSize().ifPresent(gzipHandler::setMinGzipSize);
        gzipConfiguration.isSyncFlush().ifPresent(gzipHandler::setSyncFlush);
    }

    @Override
    public GzipHandler provide() {
        return gzipHandler;
    }

    @Override
    public void dispose(GzipHandler instance) {
    }
}
