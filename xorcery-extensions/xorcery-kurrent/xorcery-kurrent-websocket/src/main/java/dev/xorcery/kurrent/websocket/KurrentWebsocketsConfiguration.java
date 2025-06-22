package dev.xorcery.kurrent.websocket;

import dev.xorcery.configuration.Configuration;

import static dev.xorcery.configuration.Configuration.missing;

public record KurrentWebsocketsConfiguration(Configuration configuration) {

    public static KurrentWebsocketsConfiguration get(Configuration configuration){
        return new KurrentWebsocketsConfiguration(configuration.getConfiguration("kurrent.websockets"));
    }

    public String readClient(){
        return configuration.getString("readClient").orElseThrow(missing("readClient"));
    }

    public String writeClient(){
        return configuration.getString("writeClient").orElseThrow(missing("writeClient"));
    }

    public int prefetch(){
        return configuration.getInteger("prefetch").orElse(1024);
    }
}
