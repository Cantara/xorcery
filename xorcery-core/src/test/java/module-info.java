open module xorcery.core.test {
    exports com.exoreaction.xorcery.core.test.util;
    requires transitive xorcery.core;

    requires transitive org.junit.jupiter.api;
    requires transitive org.hamcrest;

}