package com.exoreaction.xorcery.jetty.server;

import com.exoreaction.xorcery.util.Resources;
import org.eclipse.jetty.util.resource.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

/**
 * Locates resource files using Resources.getResource(path)
 */
public class ResourcesResource
        extends Resource {
    private final String path;

    public ResourcesResource(String path) {
        this.path = path;
    }

    @Override
    public boolean isContainedIn(Resource resource) throws MalformedURLException {
        try {
            ResourcesResource rr = ResourcesResource.class.cast(resource);
            return this.path.startsWith(rr.path);
        } catch (ClassCastException ex) {
            return false;
        }
    }

    @Override
    public void close() {

    }

    @Override
    public boolean exists() {
        return Resources.getResource(getResourcePath()).isPresent();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public URI getURI() {
        return Resources.getResource(getResourcePath()).map(url -> {
            try {
                return url.toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).orElse(null);
    }

    @Override
    public File getFile() throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Optional<URL> resource = Resources.getResource(getResourcePath());
        if (resource.isPresent()) {
            return resource.get().openStream();
        } else {
            throw new IOException("Cannot find resource:" + path);
        }
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return null;
    }

    @Override
    public boolean delete() throws SecurityException {
        return false;
    }

    @Override
    public boolean renameTo(Resource resource) throws SecurityException {
        return false;
    }

    @Override
    public String[] list() {
        return new String[0];
    }

    @Override
    public Resource addPath(String subPath) throws IOException, MalformedURLException {
        return new ResourcesResource(path.isEmpty() ? subPath : path + "/" + subPath);
    }

    private String getResourcePath()
    {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
