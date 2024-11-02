open module xorcery.domainevents.publisher.test {

    exports dev.xorcery.domainevents.test.context;

    requires xorcery.domainevents.publisher;
    requires xorcery.configuration;
    requires xorcery.core;
    requires org.junit.jupiter.api;
    requires jakarta.validation;
    requires xorcery.domainevents.api;
    requires org.glassfish.hk2.api;

}