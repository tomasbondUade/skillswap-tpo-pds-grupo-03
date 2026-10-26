package ar.edu.uade.skillswap.domain.pricing;

import ar.edu.uade.skillswap.domain.model.SkillCoins;

/** Applies one percentage discount; promotions can be composed by wrapping quotes. */
public final class PercentagePromotion extends PriceDecorator {
    private final int discountPercent;

    public PercentagePromotion(PriceQuote wrapped, int discountPercent) {
        super(wrapped);
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("El descuento porcentual debe estar entre 0 y 100");
        }
        this.discountPercent = discountPercent;
    }

    @Override
    public SkillCoins total() {
        long discountedAmount = (long) wrapped.total().amount() * (100 - discountPercent) / 100;
        return nonNegative(discountedAmount);
    }
}
