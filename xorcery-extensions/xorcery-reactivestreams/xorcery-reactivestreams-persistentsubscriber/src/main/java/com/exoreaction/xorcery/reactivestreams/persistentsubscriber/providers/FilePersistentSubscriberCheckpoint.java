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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class FilePersistentSubscriberCheckpoint
        implements PersistentSubscriberCheckpoint {
    private final Logger logger;
    private String name;
    private RandomAccessFile checkpointFile;
    private long revision;

    public FilePersistentSubscriberCheckpoint(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void init(PersistentSubscriberConfiguration configuration) throws IOException {
        name = configuration.getName();
        File file = new File(configuration.getString("checkpoint").orElseThrow(Configuration.missing("checkpoint"))).getAbsoluteFile();
        boolean exists = file.exists();

        if (!file.getParentFile().mkdirs()) {
            logger.error("Could not create directories for {}", file);
        }
        checkpointFile = new RandomAccessFile(file, "rwd");

        if (exists && checkpointFile.length() > 0) {
            byte[] bytes = new byte[(int) checkpointFile.length()];
            checkpointFile.read(bytes);
            revision = Long.parseLong(new String(bytes, StandardCharsets.UTF_8));
            logger.info("Loading existing checkpoint {} for '{}': {}", file.toString(), name);
        } else {
            logger.info("Creating new checkpoint {} for '{}", file.toString(), name);
        }
    }

    @Override
    public long getCheckpoint() throws IOException {
        return revision;
    }

    @Override
    public void setCheckpoint(long revision) throws IOException {

        checkpointFile.seek(0);
        checkpointFile.write(Long.toString(revision).getBytes(StandardCharsets.UTF_8));
        this.revision = revision;
        logger.debug("Checkpoint {} for {}", revision, name);
    }

    @Override
    public void close() throws IOException {
        checkpointFile.close();
    }
}
