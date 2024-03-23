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
package com.exoreaction.xorcery.domainevents.api.test;

import com.exoreaction.xorcery.domainevents.api.*;
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

public class MetadataEventsTest {

    @Test
    public void testSerializationDeserialization() throws IOException {
        JsonMapper mapper = new JsonMapper();

        Metadata metadata = new Metadata.Builder()
                .add(DomainEventMetadata.commandName, "CreateNewThing")
                .add(DomainEventMetadata.timestamp, 1234)
                .build();
        List<DomainEvent> domainEvents = List.of(JsonDomainEvent.event("TestEvent")
                .created("MyEntity", "someid")
                .updatedAttribute("foo", "bar")
                .addMetadata(DomainEventMetadata.reason, "Because reasons")
                .build());
        MetadataEvents metadataEvents = new MetadataEvents(metadata, domainEvents);

        {
            // Serialize it
            String serializedJson = mapper.writerFor(MetadataEvents.class).writeValueAsString(metadataEvents);

            System.out.println(serializedJson);

            // Deserialize it
            MetadataEvents deserializedMetadataEvents = mapper.readerFor(MetadataEvents.class).readValue(serializedJson);

            System.out.println(deserializedMetadataEvents);
            for (DomainEvent deserializedEvent : deserializedMetadataEvents.getEvents()) {
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

            ObjectNode node = writeObjectMapper.valueToTree(metadataEvents);
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
