package com.exoreaction.xorcery.cqrs;

import com.exoreaction.xorcery.service.forum.resources.aggregates.PostAggregate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class CommandTest {

    @Test
    public void testSerializeDeserializeCommand() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        PostAggregate.CreatePost command = new PostAggregate.CreatePost("Title", "Body");

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, command);

        System.out.println(writer);

        PostAggregate.CreatePost command2 = mapper.readValue(new StringReader(writer.toString()), PostAggregate.CreatePost.class);

        assertThat(command2.title(), equalTo("Title"));
        assertThat(command2.body(), equalTo("Body"));
    }

}