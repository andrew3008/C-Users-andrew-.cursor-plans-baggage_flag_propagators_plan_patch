package space.br1440.platform.tracing.otel.javaagent.configuration;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import lombok.experimental.UtilityClass;

/**
 * Ограничивает набор пропагаторов, для которых платформа контролирует перенос baggage.
 */
@UtilityClass
public final class PropagatorConfigurationContract {

    public static final String NONE = "none";
    public static final String PLATFORM_PROPAGATOR = "platform-trace-control";
    public static final Set<String> SUPPORTED_PROPAGATORS =
            Set.of("tracecontext", "baggage", "b3", "b3multi", PLATFORM_PROPAGATOR, NONE);

    public static void requireSupported(ConfigProperties config) {
        Objects.requireNonNull(config, "config");

        Set<String> unsupported = new TreeSet<>(config.getList(OtelSdkPropertyNames.PROPAGATORS));
        unsupported.removeAll(SUPPORTED_PROPAGATORS);
        if (!unsupported.isEmpty()) {
            throw new ConfigurationPropertyException(
                    OtelSdkPropertyNames.PROPAGATORS,
                    new ConfigurationException(OtelSdkPropertyNames.PROPAGATORS
                            + " contains unsupported propagators " + unsupported
                            + "; supported: " + new TreeSet<>(SUPPORTED_PROPAGATORS))
            );
        }
    }
}
