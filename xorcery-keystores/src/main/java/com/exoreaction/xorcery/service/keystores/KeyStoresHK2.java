package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Service(name = "keystores")
@ContractsProvided({com.exoreaction.xorcery.service.keystores.KeyStores.class})
public class KeyStoresHK2 extends com.exoreaction.xorcery.service.keystores.KeyStores {

    private final Topic<KeyStore> keyStoreTopic;

    @Inject
    public KeyStoresHK2(Configuration configuration, Topic<KeyStore> keyStoreTopic) throws NoSuchAlgorithmException, NoSuchProviderException {
        super(configuration);
        this.keyStoreTopic = keyStoreTopic;
    }

    @Override
    protected void publish(KeyStore keyStore) {
        keyStoreTopic.publish(keyStore);
    }
}
