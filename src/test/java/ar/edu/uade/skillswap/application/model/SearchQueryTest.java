package ar.edu.uade.skillswap.application.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.model.DiscoveryData.PageRequest;
import ar.edu.uade.skillswap.application.model.DiscoveryData.PriceRange;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import org.junit.jupiter.api.Test;

class SearchQueryTest {
    @Test
    void aplicaValoresPorDefectoCuandoNoHayCriterios() {
        SearchQuery query = new SearchQuery(null, null, null, null, null, null, null, null);

        assertEquals("", query.text());
        assertNull(query.category());
        assertNull(query.modality());
        assertNull(query.skill());
        assertEquals(PriceRange.ANY, query.price());
        assertNull(query.minimumReputation());
        assertEquals(SearchOrder.RELEVANCE, query.order());
        assertEquals(PageRequest.FIRST, query.page());
        assertEquals(20, query.page().size());
    }

    @Test
    void recortaEspaciosDeTextoCategoriaYHabilidad() {
        SearchQuery query = new SearchQuery("  java para entrevistas ", " Tecnología ", null, " Java ",
                null, null, null, null);

        assertEquals("java para entrevistas", query.text());
        assertEquals("Tecnología", query.category());
        assertEquals("Java", query.skill());
    }

    @Test
    void rechazaTextoMasLargoQueElMaximo() {
        String limite = "a".repeat(SearchQuery.MAX_TEXT_LENGTH);
        assertDoesNotThrow(() -> query(limite));
        assertThrows(IllegalArgumentException.class, () -> query(limite + "a"));
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
    void elRangoDePrecioEsCerradoYNoAdmiteNegativosNiInversion() {
        PriceRange range = new PriceRange(100, 200);
        assertTrue(range.contains(100));
        assertTrue(range.contains(200));
        assertFalse(range.contains(99));
        assertFalse(range.contains(201));
        assertTrue(new PriceRange(150, 150).contains(150));
        assertTrue(PriceRange.ANY.contains(0));
        assertTrue(new PriceRange(null, 10).contains(0));
        assertFalse(new PriceRange(10, null).contains(9));

        assertThrows(IllegalArgumentException.class, () -> new PriceRange(-1, null));
        assertThrows(IllegalArgumentException.class, () -> new PriceRange(null, -1));
        assertThrows(IllegalArgumentException.class, () -> new PriceRange(201, 200));
    }

    @Test
    void laPaginaValidaLimitesTamanoMaximoYDesborde() {
        assertDoesNotThrow(() -> new PageRequest(0, 1));
        assertDoesNotThrow(() -> new PageRequest(3, PageRequest.MAX_SIZE));
        assertEquals(60, new PageRequest(3, 20).offset());

        assertThrows(IllegalArgumentException.class, () -> new PageRequest(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, PageRequest.MAX_SIZE + 1));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(Integer.MAX_VALUE, 2));
    }

    private static SearchQuery query(String text) {
        return new SearchQuery(text, null, null, null, null, null, null, null);
    }

    private static SearchQuery withReputation(double reputation) {
        return new SearchQuery(null, null, null, null, null, reputation, null, null);
    }
}
