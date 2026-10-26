package ar.edu.uade.skillswap.domain.pricing;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.util.Objects;

/** The undecorated price of a publication or service. */
public record BasePrice(SkillCoins total) implements PriceQuote {
    public BasePrice {
        Objects.requireNonNull(total, "El precio base es obligatorio");
    }
}
