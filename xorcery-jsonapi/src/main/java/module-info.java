open module xorcery.jsonapi {
    exports com.exoreaction.xorcery.jsonapi.jaxrs;
    exports com.exoreaction.xorcery.jsonapi.jaxrs.providers;
    exports com.exoreaction.xorcery.jsonapi.model;
    exports com.exoreaction.xorcery.jsonapischema.model;

    requires transitive xorcery.jsonschema;

    requires xorcery.util;

    requires com.fasterxml.jackson.core;
    requires org.apache.commons.lang3;
    requires jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;
}