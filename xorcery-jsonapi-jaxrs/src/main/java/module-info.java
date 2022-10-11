open module xorcery.jsonapi.jaxrs {
    exports com.exoreaction.xorcery.jsonapi.jaxrs.providers;

    requires xorcery.jsonapi;

    requires jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
}