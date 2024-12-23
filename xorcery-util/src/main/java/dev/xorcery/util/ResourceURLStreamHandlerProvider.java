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
package dev.xorcery.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;
import java.nio.file.NoSuchFileException;
import java.util.Optional;

/**
 * Implementation of the resource: URL schema to allow lookup of resources in the application.
 * Delegates to {@link Resources#getResource(String)} for the actual lookup.
 */
public class ResourceURLStreamHandlerProvider
        extends URLStreamHandlerProvider {
    private static final ResourceURLStreamHandler INSTANCE = new ResourceURLStreamHandler();

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return protocol.equals("resource") ? INSTANCE : null;
    }

    private static class ResourceURLStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL url) throws IOException {

            String path;
            String file = url.getFile();
            String host = url.getHost();
            path = host + file;

            Optional<URL> resource = Resources.getResource(path);
            if (resource.isEmpty()) {
                throw new NoSuchFileException(path);
            }

            return resource.get().openConnection();
        }
    }
}
