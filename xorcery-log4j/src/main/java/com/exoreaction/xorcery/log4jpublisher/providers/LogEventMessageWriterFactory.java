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
package com.exoreaction.xorcery.log4jpublisher.providers;

import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LogEventMessageWriterFactory
        implements MessageWriter.Factory {

    private final JsonTemplateLayout layout;

    public LogEventMessageWriterFactory(InstanceConfiguration instanceConfiguration) throws IOException {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        String eventTemplate = new String(JsonTemplateLayout.class.getResourceAsStream("/JsonLayout.json").readAllBytes(), StandardCharsets.UTF_8);
        layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(lc.getConfiguration())
                .setEventTemplate(eventTemplate)
                .setEventTemplateAdditionalFields(List.of(JsonTemplateLayout.EventTemplateAdditionalField.newBuilder()
                        .setKey("instanceId").setValue(instanceConfiguration.getId())
                        .build()).toArray(new JsonTemplateLayout.EventTemplateAdditionalField[0]))
                .build();
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (LogEvent.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<LogEvent> {
        @Override
        public void writeTo(LogEvent instance, OutputStream out) throws IOException {
            out.write(layout.toByteArray(instance));
        }
    }
}