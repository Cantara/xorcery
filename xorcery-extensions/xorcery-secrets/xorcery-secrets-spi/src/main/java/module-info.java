import com.exoreaction.xorcery.secrets.providers.EnvSecretsProvider;
import com.exoreaction.xorcery.secrets.providers.SecretSecretsProvider;
import com.exoreaction.xorcery.secrets.providers.SystemPropertiesSecretsProvider;
import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

module xorcery.secrets.spi {
    exports com.exoreaction.xorcery.secrets.spi;
    exports com.exoreaction.xorcery.secrets.providers;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;

    provides SecretsProvider with EnvSecretsProvider, SecretSecretsProvider, SystemPropertiesSecretsProvider;
}