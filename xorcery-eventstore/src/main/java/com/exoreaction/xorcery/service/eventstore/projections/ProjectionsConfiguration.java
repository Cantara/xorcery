package com.exoreaction.xorcery.service.eventstore.projections;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

public record ProjectionsConfiguration(Configuration context)
        implements ServiceConfiguration {
    public Iterable<Projection> getProjections() {
        return context.getObjectListAs("projections", Projection::new).orElseThrow(() ->
                new IllegalStateException("Missing eventstore.projections.projections configuration"));
    }
}
