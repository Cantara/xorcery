module xorcery.domainevents.entity {
    exports dev.xorcery.domainevents.command;
    exports dev.xorcery.domainevents.command.annotation;
    exports dev.xorcery.domainevents.context;
    exports dev.xorcery.domainevents.entity;

    requires xorcery.domainevents.api;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}