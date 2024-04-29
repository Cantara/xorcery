package com.exoreaction.xorcery.reactivestreams.api.server;

public record ServerWebSocketOptions(int maxOutgoingFrames) {

    private static final ServerWebSocketOptions INSTANCE = new Builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public static ServerWebSocketOptions instance() {
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

        public ServerWebSocketOptions build()
        {
            return new ServerWebSocketOptions(maxOutgoingFrames);
        }
    }
}
