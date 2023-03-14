open module xorcery.disruptor {
    exports com.exoreaction.xorcery.disruptor;
    exports com.exoreaction.xorcery.disruptor.handlers;

    requires xorcery.configuration.api;
    requires transitive com.lmax.disruptor;
}