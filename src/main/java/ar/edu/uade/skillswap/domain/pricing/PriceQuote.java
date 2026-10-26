package ar.edu.uade.skillswap.domain.pricing;

import ar.edu.uade.skillswap.domain.model.SkillCoins;

/** A price calculation whose result is expressed in SkillCoins. */
public interface PriceQuote {
    SkillCoins total();
}
