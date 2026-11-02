package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.Modality;

/** Search criteria are application input, independent from UI and persistence query types. */
public final class DiscoveryData {
    private DiscoveryData() {}

    /** Orden de los resultados; la semántica de cada uno está en {@code docs/decision-busqueda.md}. */
    public enum SearchOrder { RELEVANCE, BEST_RATING, LOWEST_PRICE, MOST_RECENT }

    /**
     * Criterios de búsqueda del catálogo público. Un texto, categoría o habilidad vacíos y un
     * filtro nulo significan "sin filtro". Semántica completa en {@code docs/decision-busqueda.md}.
     */
    public record SearchQuery(String text, String category, Modality modality, String skill,
                              Integer minimumPrice, Integer maximumPrice,
                              Double minimumReputation, SearchOrder order,
                              int page, int size) {
        public static final int MAX_TEXT_LENGTH = 100;

        public SearchQuery {
            text = text == null ? "" : text.trim();
            category = category == null ? null : category.trim();
            skill = skill == null ? null : skill.trim();
            order = order == null ? SearchOrder.RELEVANCE : order;
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("El texto de búsqueda admite hasta " + MAX_TEXT_LENGTH + " caracteres");
            }
            if (minimumPrice != null && minimumPrice < 0) throw new IllegalArgumentException("El precio mínimo no puede ser negativo");
            if (maximumPrice != null && maximumPrice < 0) throw new IllegalArgumentException("El precio máximo no puede ser negativo");
            if (minimumPrice != null && maximumPrice != null && minimumPrice > maximumPrice) {
                throw new IllegalArgumentException("El rango de precios está invertido");
            }
            if (minimumReputation != null && (!Double.isFinite(minimumReputation)
                    || minimumReputation < 0 || minimumReputation > 5)) {
                throw new IllegalArgumentException("La reputación mínima debe estar entre 0 y 5");
            }
            if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("La página debe ser positiva y el tamaño máximo es 100");
            if ((long) page * size > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La página solicitada excede el máximo permitido");
            }
        }
    }
}
