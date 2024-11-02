package dev.xorcery.domainevents.test.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.entity.Command;
import dev.xorcery.json.JsonMerger;
import dev.xorcery.metadata.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DomainContextTest {

    private static ObjectMapper jsonMapper = new JsonMapper()
            .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static ObjectWriter writer = jsonMapper.writerWithDefaultPrettyPrinter();
    private static ObjectReader reader = jsonMapper.reader();

    @Test
    public void testCUD() throws Exception {

        try (Xorcery xorcery = new Xorcery(new ConfigurationBuilder().addTestDefaults().build())) {
            // Create Thing
            ThingCollectionDomainContext thingCollectionDomainContext = xorcery.getServiceLocator().create(ThingCollectionDomainContext.class);
            ThingCommands.CreateThing createThing = thingCollectionDomainContext.command(ThingCommands.CreateThing.class).orElseThrow();
            createThing = applyFromAPI(createThing, """
                    {"foo":"Bar"}
                    """);

            CommandResult<ThingCommands.CreateThing> result = thingCollectionDomainContext.handle(new CommandMetadata(new Metadata.Builder().build()), createThing).join();

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
            ThingDomainContext thingDomainContext = xorcery.getServiceLocator().create(ThingDomainContext.class);
            thingDomainContext.bind(new ThingEntity.ThingSnapshot("1234", "Bar"));
            ThingCommands.UpdateThing updateThing = thingDomainContext.command(ThingCommands.UpdateThing.class).orElseThrow();
            updateThing = applyFromAPI(updateThing, """
                    {"foo":"Foo"}
                    """);
            CommandResult<ThingCommands.UpdateThing> updateResult = thingDomainContext.handle(new CommandMetadata(new Metadata.Builder().build()), updateThing).join();

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
