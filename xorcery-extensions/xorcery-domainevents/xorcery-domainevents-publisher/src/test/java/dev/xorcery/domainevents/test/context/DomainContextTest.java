package dev.xorcery.domainevents.test.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.collections.MapElement;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.json.JsonMerger;
import dev.xorcery.metadata.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class DomainContextTest {

    private static final ObjectMapper jsonMapper = new JsonMapper()
            .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static ObjectWriter writer = jsonMapper.writerWithDefaultPrettyPrinter();
    private static ObjectReader reader = jsonMapper.reader();

    @Test
    public void testCUD() throws Exception {

        try (Xorcery xorcery = new Xorcery(new ConfigurationBuilder().addTestDefaults().build())) {
            // Create Thing
            ThingCollectionContext thingCollectionContext = xorcery.getServiceLocator().create(ThingCollectionContext.class);
            ThingCommands.CreateThing createThing = thingCollectionContext.command(ThingCommands.CreateThing.class).orElseThrow();
            createThing = applyFromAPI(createThing, """
                    {"foo":"Bar"}
                    """);

            CommandResult<ThingCommands.CreateThing> result = thingCollectionContext.handle(new CommandMetadata(new Metadata.Builder().build()), createThing).join();

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
            ThingContext thingContext = xorcery.getServiceLocator().create(ThingContext.Factory.class).bind("1234", new ThingModel(MapElement.element(snapshot)));
            ThingCommands.UpdateThing updateThing = thingContext.command(ThingCommands.UpdateThing.class).orElseThrow();
            updateThing = applyFromAPI(updateThing, """
                    {"foo":"Foo"}
                    """);
            CommandResult<ThingCommands.UpdateThing> updateResult = thingContext.handle(new CommandMetadata(new Metadata.Builder().build()), updateThing).join();

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
        }
    }

    private <T extends Command> T applyFromAPI(T command, String json) throws JsonProcessingException {
        ObjectNode template = jsonMapper.valueToTree(command);
        ObjectNode fromAPI = (ObjectNode) reader.readTree(json);
        ObjectNode merged = new JsonMerger().merge(template, fromAPI);
        return (T) reader.treeToValue(merged, command.getClass());
    }
}
