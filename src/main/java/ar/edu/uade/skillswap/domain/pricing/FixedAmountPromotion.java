package ar.edu.uade.skillswap.domain.pricing;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.util.Objects;

/** Subtracts a fixed SkillCoins amount without changing the wrapped quote. */
public final class FixedAmountPromotion extends PriceDecorator {
    private final SkillCoins discount;

    public FixedAmountPromotion(PriceQuote wrapped, SkillCoins discount) {
        super(wrapped);
        this.discount = Objects.requireNonNull(discount, "El descuento es obligatorio");
    }

    @Override
    public SkillCoins total() {
        return nonNegative((long) wrapped.total().amount() - discount.amount());
    }
}
