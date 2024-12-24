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
package dev.xorcery.opentelemetry.exporters.websocket;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.opentelemetry.sdk.logs.data.LogRecordData;

import java.io.IOException;

public class LogRecordDataSerializer extends StdSerializer<LogRecordData> {

    public LogRecordDataSerializer() {
        this(null);
    }

    public LogRecordDataSerializer(Class<LogRecordData> t) {
        super(t);
    }

    public void serialize(
            LogRecordData value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        try {
            jgen.writeStartObject();

            {
                jgen.writeObjectFieldStart("scope");
                jgen.writeStringField("name", value.getInstrumentationScopeInfo().getName());
                // TODO Add attributes
                jgen.writeEndObject();
            }
            {
                jgen.writeObjectFieldStart("log");
                jgen.writeStringField("timeUnixNano", Long.toString(value.getTimestampEpochNanos()));
                jgen.writeNumberField("severityNumber", value.getSeverity().getSeverityNumber());
                jgen.writeStringField("severityText", value.getSeverityText());
                if (value.getBodyValue() != null)
                {
                    jgen.writeObjectFieldStart("body");
                    jgen.writeStringField("stringValue", value.getBodyValue().asString());
                    jgen.writeEndObject();
                }
                // TODO Add attributes
                jgen.writeStringField("schemaUrl", value.getInstrumentationScopeInfo().getSchemaUrl());
                jgen.writeEndObject();
            }
            jgen.writeEndObject();
        } catch (IOException e) {
            throw e;
        }
    }
}
