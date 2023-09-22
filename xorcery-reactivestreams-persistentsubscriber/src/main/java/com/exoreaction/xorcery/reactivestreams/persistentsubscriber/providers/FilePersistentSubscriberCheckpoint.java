package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import org.apache.logging.log4j.Logger;

import java.io.*;
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
