package com.exoreaction.xorcery.jwt.server.spi;

import java.util.Map;

/**
 * Get JWT Claims for an authenticated user.
 */
public interface ClaimsProvider {
    Map<String, ?> getClaims(String userName);
}
