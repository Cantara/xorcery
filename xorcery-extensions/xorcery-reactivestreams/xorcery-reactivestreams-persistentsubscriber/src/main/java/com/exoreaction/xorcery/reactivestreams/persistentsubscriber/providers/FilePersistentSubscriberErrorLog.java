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
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FilePersistentSubscriberErrorLog
        implements PersistentSubscriberErrorLog {
    private final Logger logger;
    private String name;
    private YAMLMapper yamlMapper;
    private FileOutputStream out;

    public FilePersistentSubscriberErrorLog(Logger logger) {
        this.logger = logger;
        yamlMapper = new YAMLMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        yamlMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    @Override
    public void init(PersistentSubscriberConfiguration configuration, PersistentSubscriber persistentSubscriber) throws IOException {
        name = configuration.getName();
        File errorLog = new File(configuration.getString("errors").orElseThrow(Configuration.missing("errors"))).getAbsoluteFile();

        if (!errorLog.getParentFile().exists() && !errorLog.getParentFile().mkdirs()) {
            logger.error("Could not create directories for {} ({})", errorLog, name);
        }

        out = new FileOutputStream(errorLog, true);

        // Attempt recovery
        File recoveryLog = new File(configuration.getString("recovery").orElseThrow(Configuration.missing("recovery"))).getAbsoluteFile();
        if (recoveryLog.exists()) {
            FileInputStream recoveryIn = new FileInputStream(recoveryLog);
            JsonNode recoveryNode = yamlMapper.readTree(recoveryIn);
            recoveryIn.close();
            if (recoveryNode instanceof ArrayNode recoveredItems) {
                logger.info("Recovering {} items for subscriber '{}'", recoveredItems.size(), name);
                int recoveredCount = 0;
                int failedCount = 0;
                for (JsonNode recoveredItem : recoveredItems) {
                    MetadataJsonNode<ArrayNode> arrayNodeWithMetadata = new MetadataJsonNode<>(
                            new Metadata((ObjectNode) recoveredItem.get("metadata")),
                            (ArrayNode) recoveredItem.get("events"));

                    CompletableFuture<Void> result = new CompletableFuture<>();
                    persistentSubscriber.handle(arrayNodeWithMetadata, result);

                    try {
                        result.orTimeout(10, TimeUnit.SECONDS).join();
                        recoveredCount++;
                    } catch (Throwable e) {
                        failedCount++;
                        // Write to error log again
                        handle(arrayNodeWithMetadata, e);
                    }
                }

                logger.info("Recovered {} items, {} failed, for subscriber '{}'", recoveredCount, failedCount, name);

                // Empty out recovery file no matter what
                recoveryLog.delete();

                new FileOutputStream(recoveryLog).close(); // Just create the file as an empty file to indicate to users where to put attempts at recovery
            }
        } else {
            if (!recoveryLog.getParentFile().exists() && !recoveryLog.getParentFile().mkdirs()) {
                logger.error("Could not create directories for {} ({})", errorLog, name);
            }
            new FileOutputStream(recoveryLog).close(); // Just create the file as an empty file to indicate to users where to put attempts at recovery
        }
    }

    @Override
    public void handle(WithMetadata<ArrayNode> arrayNodeWithMetadata, Throwable exception) throws IOException {
        StringWriter exceptionWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(exceptionWriter));
        String exceptionAsString = exceptionWriter.toString();
        ObjectNode entry = JsonNodeFactory.instance.objectNode();
        entry.set("metadata", arrayNodeWithMetadata.metadata().json());
        entry.set("events", arrayNodeWithMetadata.data());
        entry.set("error", JsonNodeFactory.instance.textNode(exceptionAsString));
        ArrayNode singleEntryArray = JsonNodeFactory.instance.arrayNode(1);
        singleEntryArray.add(entry);
        yamlMapper.writeValue(out, singleEntryArray);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
