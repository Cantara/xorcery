package com.exoreaction.xorcery.domainevents.api.test;

import com.exoreaction.xorcery.domainevents.api.*;
import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CommandEventsTest {

    @Test
    public void testSerializationDeserialization() throws JsonProcessingException {
        JsonMapper mapper = new JsonMapper();

        Metadata metadata = new Metadata.Builder()
                .add(Model.Metadata.commandName, "CreateNewThing")
                .add(Model.Metadata.timestamp, 1234)
                .build();
        List<DomainEvent> domainEvents = List.of(JsonDomainEvent.event("TestEvent")
                .created("MyEntity", "someid")
                .attribute("foo", "bar")
                .addMetadata(Model.Metadata.reason, "Because reasons")
                .build());
        CommandEvents commandEvents = new CommandEvents(metadata, domainEvents);

        {
            // Serialize it
            String serializedJson = mapper.writerFor(CommandEvents.class).writeValueAsString(commandEvents);

            System.out.println(serializedJson);

            // Deserialize it
            CommandEvents deserializedCommandEvents = mapper.readerFor(CommandEvents.class).readValue(serializedJson);

            System.out.println(deserializedCommandEvents);
        }

        {
            // Serialize events only
            String serializedJson = mapper.writerFor(List.class).writeValueAsString(commandEvents.getEvents());

            System.out.println(serializedJson);

            // Deserialize it
            List<DomainEvent> deserializedEvents = mapper.readerFor(List.class).readValue(serializedJson);

            System.out.println(deserializedEvents);
        }
    }
}
