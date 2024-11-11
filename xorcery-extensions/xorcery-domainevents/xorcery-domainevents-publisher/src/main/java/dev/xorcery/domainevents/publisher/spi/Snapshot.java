package dev.xorcery.domainevents.publisher.spi;

import dev.xorcery.collections.Element;

public record Snapshot(Element state, long lastUpdatedOn) {
}
