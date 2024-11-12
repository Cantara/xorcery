package dev.xorcery.test.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.collections.MapElement;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.json.JsonMerger;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4j.client.GraphResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.Map;

public class DomainContextTest {

    private static final ObjectMapper jsonMapper = new JsonMapper()
            .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static ObjectWriter writer = jsonMapper.writerWithDefaultPrettyPrinter();
    private static ObjectReader reader = jsonMapper.reader();

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    jsondomaineventprojection.enabled: true
                    domainevents.commandhandler.default.enabled: true
                    domainevents.eventpublisher.default.enabled: true
                    neo4jdatabase.enabled: true
                    neo4jprojections.updates.enabled: true
                    """)
            .build();

    @Test
    public void testCUD(ThingCollectionContext thingCollectionContext, GraphDatabase graphDatabase) throws Exception {

            // Create Thing
            ThingCommands.CreateThing createThing = thingCollectionContext.command(ThingCommands.CreateThing.class).orElseThrow();
            createThing = applyFromAPI(createThing, """
                    {"foo":"Bar"}
                    """);

            CommandResult result = thingCollectionContext.apply(new CommandMetadata(new Metadata.Builder().build()), createThing).join();

            Assertions.assertEquals("""
                            [ {
                              "event" : "CreatedThing",
                              "created" : {
                                "type" : "Thing",
                                "id" : "1234"
                              },
                              "attributes" : {
                                "foo" : "Bar"
                              }
                            } ]""",
                    writer.writeValueAsString(result.events()).replace("\r", ""));

            // Update thing
            Map<String, Object> snapshot = Map.of("foo", "Bar");
            ThingContext thingContext = xorcery.getServiceLocator().getService(ThingContext.Factory.class).bind("1234", new ThingModel(MapElement.element(snapshot)));
            ThingCommands.UpdateThing updateThing = thingContext.command(ThingCommands.UpdateThing.class).orElseThrow();
            updateThing = applyFromAPI(updateThing, """
                    {"foo":"Foo"}
                    """);
            CommandResult updateResult = thingContext.apply(new CommandMetadata(new Metadata.Builder().build()), updateThing).join();

            Assertions.assertEquals("""
                            [ {
                              "event" : "UpdatedThing",
                              "updated" : {
                                "type" : "Thing",
                                "id" : "1234"
                              },
                              "attributes" : {
                                "foo" : "Foo"
                              }
                            } ]""",
                    writer.writeValueAsString(updateResult.events()).replace("\r", ""));

            // Read thing
        try (GraphResult queryResult = graphDatabase.execute("MATCH (node:Thing) RETURN properties(node)", Collections.emptyMap(), 90).join())
        {
            System.out.println(queryResult.getResult().resultAsString());
        }
    }

    private <T extends Command> T applyFromAPI(T command, String json) throws JsonProcessingException {
        ObjectNode template = jsonMapper.valueToTree(command);
        ObjectNode fromAPI = (ObjectNode) reader.readTree(json);
        ObjectNode merged = new JsonMerger().merge(template, fromAPI);
        return (T) reader.treeToValue(merged, command.getClass());
    }
}
