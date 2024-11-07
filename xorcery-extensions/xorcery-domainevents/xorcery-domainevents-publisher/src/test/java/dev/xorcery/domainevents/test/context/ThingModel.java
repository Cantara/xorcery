package dev.xorcery.domainevents.test.context;

import dev.xorcery.collections.Element;

public record ThingModel(Element element) {
    String getFoo() {
        return element.getString("foo").orElseThrow();
    }
}
