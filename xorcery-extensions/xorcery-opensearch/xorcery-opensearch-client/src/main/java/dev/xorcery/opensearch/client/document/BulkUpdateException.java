package dev.xorcery.opensearch.client.document;

import java.io.IOException;

public class BulkUpdateException
    extends IOException
{
    public BulkUpdateException(String message) {
        super(message);
    }
}
