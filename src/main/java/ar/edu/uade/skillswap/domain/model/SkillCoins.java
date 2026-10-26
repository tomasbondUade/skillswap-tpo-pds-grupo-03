package ar.edu.uade.skillswap.domain.model;

/** Immutable amount in SkillSwap's fictional currency. */
public record SkillCoins(int amount) {
    public SkillCoins {
        if (amount < 0) {
            throw new IllegalArgumentException("SkillCoins no puede ser negativo");
        }
    }

    public static SkillCoins zero() {
        return new SkillCoins(0);
    }
}
