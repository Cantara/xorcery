package com.exoreaction.xorcery.service.opensearch.client.document;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record BulkResponseItem(ObjectNode json)
    implements JsonElement
{
    public long getSequenceNr()
    {
        return getLong("create._seq_no").orElseThrow();
    }
}
