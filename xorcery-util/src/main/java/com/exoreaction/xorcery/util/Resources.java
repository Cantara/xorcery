package com.exoreaction.xorcery.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resource loading helper. Checks JPMS system resources first, then classloader of this class.
 *
 * For single resources it also tries to find it as a file.
 */
public class Resources {

    public static Optional<URL> getResource(String path) {
        URL resource = ClassLoader.getSystemResource(path);
        if (resource != null)
            return Optional.of(resource);
        resource = Resources.class.getClassLoader().getResource(path);
        if (resource != null)
            return Optional.of(resource);
        // Check if file
        File file = new File(path);
        if (file.exists())
        {
            try {
                return Optional.of(file.toURI().toURL());
            } catch (MalformedURLException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static List<URL> getResources(String path)
            throws UncheckedIOException {
        try {
            List<URL> resources = new ArrayList<>();
            ClassLoader.getSystemResources(path).asIterator().forEachRemaining(resources::add);
            Resources.class.getClassLoader().getResources(path).asIterator().forEachRemaining(url ->
            {
                if (!resources.contains(url))
                {
                    resources.add(url);
                }
            });
            return resources;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
