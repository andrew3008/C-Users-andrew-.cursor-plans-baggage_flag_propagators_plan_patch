package space.br1440.platform.tracing.otel.javaagent.propagation;

import java.util.Collection;
import java.util.Objects;
import jakarta.annotation.Nonnull;

import space.br1440.platform.tracing.api.propagation.PlatformBaggageKeys;
import space.br1440.platform.tracing.otel.javaagent.configuration.BaggageKeyPolicy;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

final class PlatformBaggagePropagator implements TextMapPropagator {

    private final TextMapPropagator delegate;
    private final BaggageKeyPolicy outboundPolicy;

    PlatformBaggagePropagator(TextMapPropagator delegate, BaggageKeyPolicy outboundPolicy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.outboundPolicy = Objects.requireNonNull(outboundPolicy, "outboundPolicy");
    }

    @Override
    public Collection<String> fields() {
        return delegate.fields();
    }

    @Override
    public <C> Context extract(@Nonnull Context context, C carrier, @Nonnull TextMapGetter<C> getter) {
        Objects.requireNonNull(context, "context");
        Context extracted = Objects.requireNonNull(delegate.extract(context, carrier, getter), "extractedContext");
        return filterBaggage(extracted, false);
    }

    @Override
    public <C> void inject(@Nonnull Context context, C carrier, @Nonnull TextMapSetter<C> setter) {
        Objects.requireNonNull(context, "context");
        delegate.inject(filterBaggage(context, true), carrier, setter);
    }

    private Context filterBaggage(Context context, boolean outbound) {
        Baggage baggage = Baggage.fromContext(context);
        BaggageBuilder filtered = null;
        for (String key : baggage.asMap().keySet()) {
            if (PlatformBaggageKeys.CORRELATION_ID.equalsIgnoreCase(key)
                    || outbound && !outboundPolicy.allows(key)) {
                if (filtered == null) {
                    filtered = baggage.toBuilder();
                }
                filtered.remove(key);
            }
        }
        return filtered == null ? context : context.with(filtered.build());
    }
}
