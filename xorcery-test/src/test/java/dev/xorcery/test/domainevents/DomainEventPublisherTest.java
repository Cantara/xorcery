package dev.xorcery.test.domainevents;

import dev.xorcery.collections.Element;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.entity.Entity;
import dev.xorcery.domainevents.publisher.api.CommandHandler;
import dev.xorcery.domainevents.publisher.spi.EntitySnapshotProvider;
import dev.xorcery.domainevents.publisher.spi.EventProjectionProvider;
import dev.xorcery.domainevents.publisher.spi.EventPublisherProvider;
import dev.xorcery.domainevents.publisher.spi.Snapshot;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.concurrent.CompletableFuture;

public class DomainEventPublisherTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .with(new MockEntitySnapshotProvider())
            .with(new MockEventProjectionProvider())
            .with(new MockEventPublisherProvider())
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    domainevents.commandhandler.default.enabled: true
                    domainevents.eventpublisher.default.enabled: true
                    """)
            .build();

    @Test
    public void testPublishDomainEvents() {

        CommandHandler commandHandler = xorcery.getServiceLocator().getService(CommandHandler.class);

        CommandMetadata commandMetadata = new CommandMetadata.Builder(new Metadata.Builder())
                .build();

        CommandResult result = commandHandler.handle(new TestEntity(), commandMetadata, new TestCommand("123", "bar")).join();

        System.out.println(result);
    }

    public static class TestEntity
            extends Entity {

        void handle(TestCommand command)
        {
            add(JsonDomainEvent.event("TestEvent").created("TestEntity", command.id).build());
        }
    }

    public record TestCommand(String id, String foo)
            implements Command {
    }

    @ContractsProvided(EntitySnapshotProvider.class)
    public record MockEntitySnapshotProvider()
            implements EntitySnapshotProvider
    {
        @Override
        public CompletableFuture<Snapshot> snapshotFor(CommandMetadata metadata, Command command, Entity entity) {
            return CompletableFuture.completedFuture(new Snapshot(Element.empty(), -1));
        }

        @Override
        public CompletableFuture<Boolean> snapshotExists(CommandMetadata metadata, Command command, Entity entity) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @ContractsProvided(EventPublisherProvider.class)
    public record MockEventPublisherProvider()
            implements EventPublisherProvider
    {
        @Override
        public Publisher<Metadata> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {
            return metadataEventsFlux.map(WithMetadata::metadata);
        }
    }

    @ContractsProvided(EventProjectionProvider.class)
    public record MockEventProjectionProvider()
            implements EventProjectionProvider
    {
        @Override
        public Publisher<Metadata> apply(Flux<Metadata> metadataFlux, ContextView contextView) {
            return metadataFlux;
        }
    }
}
