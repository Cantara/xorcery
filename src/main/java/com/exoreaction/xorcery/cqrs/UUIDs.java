package com.exoreaction.xorcery.cqrs;

import java.util.UUID;

/**
 * UUIDs without the dashes. For prettier URLs.
 *
 */
public interface UUIDs {
    static String newId()
    {
        return UUID.randomUUID().toString().replace( "-", "" );
    }
}
