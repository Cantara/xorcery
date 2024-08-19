module xorcery.translation.deepl {
    exports com.exoreaction.xorcery.translation.deepl;
    exports com.exoreaction.xorcery.translation.deepl.providers;

    requires xorcery.configuration;
    requires xorcery.configuration.api;
    requires xorcery.translation.api;
    requires xorcery.secrets.api;

    requires deepl.java;
    requires xorcery.secrets.spi;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;

    provides com.exoreaction.xorcery.configuration.spi.ResourceBundleTranslationProvider
            with com.exoreaction.xorcery.translation.deepl.providers.DeepLResourceBundleTranslationProvider;
}