package com.exoreaction.xorcery.secrets.spi;

import java.io.IOException;

public interface SecretsProvider {

    String getSecretString(String name)
            throws IOException;

    byte[] getSecretBytes(String name)
            throws IOException;

    void refreshSecret(String name)
            throws IOException;
}
