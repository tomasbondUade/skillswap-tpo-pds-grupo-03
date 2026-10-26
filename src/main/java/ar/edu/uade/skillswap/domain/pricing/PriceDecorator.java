package ar.edu.uade.skillswap.domain.pricing;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.util.Objects;

/** Base class for promotions that wrap and transform another price quote. */
public abstract class PriceDecorator implements PriceQuote {
    protected final PriceQuote wrapped;

    protected PriceDecorator(PriceQuote wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "La cotización a decorar es obligatoria");
    }

    protected final SkillCoins nonNegative(long amount) {
        return new SkillCoins((int) Math.max(0L, amount));
    }
}
