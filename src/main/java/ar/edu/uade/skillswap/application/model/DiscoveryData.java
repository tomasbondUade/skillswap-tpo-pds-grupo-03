package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.Modality;

/** Search criteria are application input, independent from UI and persistence query types. */
public final class DiscoveryData {
    private DiscoveryData() {}

    public enum SearchOrder { RELEVANCE, BEST_RATING, LOWEST_PRICE, MOST_RECENT }

    public record SearchQuery(String text, String category, Modality modality,
                              Integer minimumPrice, Integer maximumPrice,
                              Double minimumReputation, SearchOrder order,
                              int page, int size) {
        public SearchQuery {
            text = text == null ? "" : text.trim();
            category = category == null ? null : category.trim();
            order = order == null ? SearchOrder.RELEVANCE : order;
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
        }
    }
}
