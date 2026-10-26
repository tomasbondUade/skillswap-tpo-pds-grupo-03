package ar.edu.uade.skillswap.domain.pricing;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromotionDecoratorTest {
    @Test
    void appliesOnePromotionToTheBasePrice() {
        PriceQuote quote = new PercentagePromotion(new BasePrice(new SkillCoins(100)), 20);

        assertEquals(new SkillCoins(80), quote.total());
    }

    @Test
    void appliesStackedPromotionsInOrder() {
        PriceQuote base = new BasePrice(new SkillCoins(100));
        PriceQuote first = new PercentagePromotion(base, 20);
        PriceQuote stacked = new FixedAmountPromotion(first, new SkillCoins(10));

        assertEquals(new SkillCoins(70), stacked.total());
        assertEquals(new SkillCoins(100), base.total());
    }

    @Test
    void clampsAccumulatedDiscountsAtZero() {
        PriceQuote base = new BasePrice(new SkillCoins(100));
        PriceQuote first = new FixedAmountPromotion(base, new SkillCoins(150));
        PriceQuote stacked = new PercentagePromotion(first, 50);

        assertEquals(SkillCoins.zero(), stacked.total());
        assertEquals(new SkillCoins(100), base.total());
    }

    @Test
    void rejectsNegativeSkillCoinsAndInvalidPercentages() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new PercentagePromotion(new BasePrice(new SkillCoins(100)), 101));
    }
}
