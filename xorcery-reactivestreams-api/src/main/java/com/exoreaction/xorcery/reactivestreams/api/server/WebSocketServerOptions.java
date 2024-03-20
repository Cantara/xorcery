package com.exoreaction.xorcery.reactivestreams.api.server;

public record WebSocketServerOptions(int maxOutgoingFrames) {

    private static final WebSocketServerOptions INSTANCE = new Builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public static WebSocketServerOptions instance() {
        return INSTANCE;
    }

    public static class Builder {
        int maxOutgoingFrames = -1;

        private Builder() {
        }

        public Builder maxOutgoingFrames(int value) {
            this.maxOutgoingFrames = value;
            return this;
        }

        public WebSocketServerOptions build()
        {
            return new WebSocketServerOptions(maxOutgoingFrames);
        }
    }
}
