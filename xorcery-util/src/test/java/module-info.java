open module xorcery.util.test {
    exports dev.xorcery.concurrent.test;
    exports dev.xorcery.function.test;
    exports dev.xorcery.util.test;

    requires xorcery.util;
    requires org.junit.jupiter.api;
    requires org.apache.logging.log4j.jul;

}