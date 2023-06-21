package com.exoreaction.xorcery.net;

import java.net.URI;
import java.net.URISyntaxException;

public interface URIs {
    static URI withScheme(URI uri, String scheme) {
        try {
            return new URI(
                    scheme,
                    uri.getAuthority(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
}
