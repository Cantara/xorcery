package com.exoreaction.xorcery.core;

import org.glassfish.hk2.api.DescriptorFileFinder;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sometimes the classpath finder returns the same file twice (noooo idea why).
 * This wrapper simply ensures the descriptors are deduped.
 * @param classpathDescriptorFileFinder
 */
record UniqueDescriptorFileFinder(ClasspathDescriptorFileFinder classpathDescriptorFileFinder)
        implements DescriptorFileFinder {
    @Override
    public List<InputStream> findDescriptorFiles() throws IOException {

        List<InputStream> descriptorStreams = classpathDescriptorFileFinder.findDescriptorFiles();
        List<String> descriptorIdentifiers = classpathDescriptorFileFinder.getDescriptorFileInformation();
        Set<String> uniqueIds = new HashSet<>();
        List<InputStream> uniqueDescriptorStreams = new ArrayList<>();
        for (int i = 0; i < descriptorIdentifiers.size(); i++) {
            String id = descriptorIdentifiers.get(i);
            if (uniqueIds.add(id)) {
                uniqueDescriptorStreams.add(descriptorStreams.get(i));
            }
        }
        return uniqueDescriptorStreams;
    }
}
