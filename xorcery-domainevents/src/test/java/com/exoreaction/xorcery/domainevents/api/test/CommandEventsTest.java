package com.exoreaction.xorcery.domainevents.api.test;

import com.exoreaction.xorcery.domainevents.api.CommandEvents;
import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.domainevents.api.JsonDomainEvent;
import com.exoreaction.xorcery.domainevents.api.Model;
import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class CommandEventsTest {

    @Test
    public void testSerializationDeserialization() throws IOException {
        JsonMapper mapper = new JsonMapper();

        Metadata metadata = new Metadata.Builder()
                .add(Model.Metadata.commandName, "CreateNewThing")
                .add(Model.Metadata.timestamp, 1234)
                .build();
        List<DomainEvent> domainEvents = List.of(JsonDomainEvent.event("TestEvent")
                .created("MyEntity", "someid")
                .updatedAttribute("foo", "bar")
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
            for (DomainEvent deserializedEvent : deserializedCommandEvents.getEvents()) {
                System.out.println(deserializedEvent);
            }
        }

/*
        {
            // Serialize events only
            String serializedJson = mapper.writerFor(mapper.getTypeFactory().constructCollectionType(List.class, DomainEvent.class)).writeValueAsString(commandEvents.getEvents());

            System.out.println(serializedJson);

            // Deserialize it
            List<DomainEvent> deserializedEvents = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, DomainEvent.class)).readValue(serializedJson);

            for (DomainEvent deserializedEvent : deserializedEvents) {
                System.out.println(deserializedEvent);
            }
        }
*/

        {
            ObjectMapper writeObjectMapper = new ObjectMapper(new YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
//            writeObjectMapper.activateDefaultTyping(writeObjectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);

            OutputStreamWriter writer = new OutputStreamWriter(System.out);

            ObjectNode node = writeObjectMapper.valueToTree(commandEvents);
//            System.out.println(node);
            ArrayNode singleElementArray = JsonNodeFactory.instance.arrayNode();
            singleElementArray.add(node);
            writeObjectMapper.writeValue(writer, singleElementArray);
//            String eventsNode = mapper.writeValueAsString(commandEvents.getEvents());
//            System.out.println(eventsNode);
//            System.out.println(mapper.valueToTree(commandEvents.getEvents()));
//            System.out.println(mapper.writeValueAsString(commandEvents.getEvents()));
        }
    }
}
