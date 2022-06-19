package com.exoreaction.reactiveservices.service.eventstore.resources.api;

import com.exoreaction.reactiveservices.jsonapi.schema.annotations.AttributeSchema;

public class EventStoreParameters
{
        @AttributeSchema(title="Stream name", description = "Name of the stream to subscribe to", required = true)
        public String stream;
        @AttributeSchema(title="From position", description = "Position to subscribe from. If omitted then read from beginning of stream", required = false)
        public long from;
}
