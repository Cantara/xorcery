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
package dev.xorcery.core.hk2;

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
public record UniqueDescriptorFileFinder(ClasspathDescriptorFileFinder classpathDescriptorFileFinder)
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
