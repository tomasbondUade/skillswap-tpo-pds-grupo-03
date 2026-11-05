package ar.edu.uade.skillswap.application.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import org.junit.jupiter.api.Test;

class SearchQueryTest {
    @Test
    void aplicaValoresPorDefectoCuandoNoHayCriterios() {
        SearchQuery query = new SearchQuery(null, null, null, null, null, null, null, null, 0, 20);

        assertEquals("", query.text());
        assertNull(query.category());
        assertNull(query.modality());
        assertNull(query.skill());
        assertNull(query.minimumPrice());
        assertNull(query.maximumPrice());
        assertNull(query.minimumReputation());
        assertEquals(SearchOrder.RELEVANCE, query.order());
    }

    @Test
    void recortaEspaciosDeTextoCategoriaYHabilidad() {
        SearchQuery query = new SearchQuery("  java para entrevistas ", " Tecnología ", null, " Java ",
                null, null, null, null, 0, 20);

        assertEquals("java para entrevistas", query.text());
        assertEquals("Tecnología", query.category());
        assertEquals("Java", query.skill());
    }

    @Test
    void rechazaTextoMasLargoQueElMaximo() {
        String limite = "a".repeat(SearchQuery.MAX_TEXT_LENGTH);
        assertDoesNotThrow(() -> withText(limite));
        assertThrows(IllegalArgumentException.class, () -> withText(limite + "a"));
    }

    @Test
    void validaLaReputacionMinimaEnElRangoCeroACinco() {
        assertDoesNotThrow(() -> withReputation(0.0));
        assertDoesNotThrow(() -> withReputation(5.0));
        assertThrows(IllegalArgumentException.class, () -> withReputation(-0.1));
        assertThrows(IllegalArgumentException.class, () -> withReputation(5.1));
        assertThrows(IllegalArgumentException.class, () -> withReputation(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> withReputation(Double.POSITIVE_INFINITY));
    }

    @Test
    void validaElRangoDePrecio() {
        assertDoesNotThrow(() -> withPrices(150, 150));
        assertDoesNotThrow(() -> withPrices(null, 10));
        assertDoesNotThrow(() -> withPrices(0, null));
        assertThrows(IllegalArgumentException.class, () -> withPrices(-1, null));
        assertThrows(IllegalArgumentException.class, () -> withPrices(null, -1));
        assertThrows(IllegalArgumentException.class, () -> withPrices(201, 200));
    }

    @Test
    void validaLimitesDePaginaTamanoMaximoYDesborde() {
        assertDoesNotThrow(() -> withPage(0, 1));
        assertDoesNotThrow(() -> withPage(3, 100));
        assertThrows(IllegalArgumentException.class, () -> withPage(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> withPage(0, 0));
        assertThrows(IllegalArgumentException.class, () -> withPage(0, 101));
        assertThrows(IllegalArgumentException.class, () -> withPage(Integer.MAX_VALUE, 2));
    }

    private static SearchQuery withText(String text) {
        return new SearchQuery(text, null, null, null, null, null, null, null, 0, 20);
    }

    private static SearchQuery withReputation(double reputation) {
        return new SearchQuery(null, null, null, null, null, null, reputation, null, 0, 20);
    }

    private static SearchQuery withPrices(Integer minimum, Integer maximum) {
        return new SearchQuery(null, null, null, null, minimum, maximum, null, null, 0, 20);
    }

    private static SearchQuery withPage(int page, int size) {
        return new SearchQuery(null, null, null, null, null, null, null, null, page, size);
    }
}
