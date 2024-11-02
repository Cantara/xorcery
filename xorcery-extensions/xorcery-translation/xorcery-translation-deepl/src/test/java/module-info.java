open module xorcery.translation.deepl.test {

    uses dev.xorcery.configuration.resourcebundle.spi.ResourceBundlesProvider;

    requires xorcery.translation.deepl;
    requires xorcery.configuration;
    requires xorcery.junit;
    requires xorcery.translation.api;

    requires xorcery.core;
    requires xorcery.log4j;
}