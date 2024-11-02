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
