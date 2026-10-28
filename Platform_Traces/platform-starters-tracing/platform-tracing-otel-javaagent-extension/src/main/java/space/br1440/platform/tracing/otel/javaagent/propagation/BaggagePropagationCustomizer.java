package space.br1440.platform.tracing.otel.javaagent.propagation;

import space.br1440.platform.tracing.otel.javaagent.configuration.BaggageExtensionConfig;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;

public final class BaggagePropagationCustomizer {

    public TextMapPropagator apply(TextMapPropagator propagator, BaggageExtensionConfig baggage) {
        if (!(propagator instanceof W3CBaggagePropagator)) {
            return propagator;
        }

        if (!baggage.propagationEnabled()) {
            return TextMapPropagator.noop();
        }

        return new PlatformBaggagePropagator(
                new SafeTextMapPropagator(propagator),
                baggage.outboundHeaderPolicy()
        );
    }
}
