module xorcery.jgroups {
    exports dev.xorcery.jgroups;

    requires xorcery.configuration.api;

    requires org.jgroups;
    requires jakarta.inject;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.api;
}