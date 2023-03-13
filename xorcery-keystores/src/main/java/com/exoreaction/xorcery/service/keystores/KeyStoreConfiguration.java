package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URL;

public record KeyStoreConfiguration(String name, Configuration configuration) {
    public URL getURL() {
        return configuration.getResourceURL("path").orElseThrow(() -> new IllegalArgumentException("Missing " + name + ".path"));
    }

    public char[] getPassword() {
        return configuration.getString("password").map(String::toCharArray).orElse(null);
    }

    public String getType() {
        return configuration.getString("type").orElse("PKCS12");
    }

    public String getPath() {
        return configuration.getString("path").orElseThrow(() -> new IllegalArgumentException("Missing " + name + ".path"));
    }

    public boolean isAddRootCa() {
        return configuration.getBoolean("addRootCa").orElse(false);
    }

    public String getTemplate() {
        return configuration.getString("template").orElse(null);
    }
}
