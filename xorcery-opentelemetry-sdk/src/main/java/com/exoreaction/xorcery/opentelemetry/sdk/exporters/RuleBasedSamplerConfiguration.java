package com.exoreaction.xorcery.opentelemetry.sdk.exporters;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record RuleBasedSamplerConfiguration(Configuration configuration) {

    public static RuleBasedSamplerConfiguration get(Configuration configuration) {
        return new RuleBasedSamplerConfiguration(configuration.getConfiguration("opentelemetry.sampler"));
    }

    public List<SampleRule> getIncludes() {
        return configuration.getObjectListAs("includes", SampleRule::create)
                .orElse(Collections.emptyList());
    }

    public List<SampleRule> getExcludes() {
        return configuration.getObjectListAs("excludes", SampleRule::create)
                .orElse(Collections.emptyList());
    }
}
