package ar.edu.uade.skillswap.domain.model;

/** Immutable amount in SkillSwap's fictional currency. */
public record SkillCoins(int amount) {
    public SkillCoins {
        if (amount < 0) {
            throw new IllegalArgumentException("SkillCoins no puede ser negativo");
        }
    }

    public SkillCoins plus(SkillCoins other) {
        return new SkillCoins(Math.addExact(amount, java.util.Objects.requireNonNull(other).amount));
    }

    public SkillCoins minus(SkillCoins other) {
        int value = java.util.Objects.requireNonNull(other).amount;
        if (value > amount) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }
        return new SkillCoins(amount - value);
    }

    public static SkillCoins zero() {
        return new SkillCoins(0);
    }
}
