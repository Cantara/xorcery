import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyWriter;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

open module xorcery.jsonapi.jaxrs {
    exports com.exoreaction.xorcery.jsonapi.jaxrs.providers;

    requires transitive xorcery.jsonapi;
    requires transitive jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
}