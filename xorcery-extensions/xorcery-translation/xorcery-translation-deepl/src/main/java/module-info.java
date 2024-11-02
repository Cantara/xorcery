module xorcery.translation.deepl {
    exports dev.xorcery.translation.deepl;
    exports dev.xorcery.translation.deepl.providers;

    requires xorcery.configuration;
    requires xorcery.configuration.api;
    requires xorcery.translation.api;
    requires xorcery.secrets.api;

    requires deepl.java;
    requires xorcery.secrets.spi;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;

    provides dev.xorcery.configuration.spi.ResourceBundleTranslationProvider
            with dev.xorcery.translation.deepl.providers.DeepLResourceBundleTranslationProvider;
}