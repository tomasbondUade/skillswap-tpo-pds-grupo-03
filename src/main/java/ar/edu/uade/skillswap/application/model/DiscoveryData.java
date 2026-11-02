package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.Modality;

/**
 * Entrada y modelos de lectura del caso de uso de búsqueda (UC-16).
 * Son independientes de la UI y de los tipos de consulta de persistencia.
 * Las validaciones lanzan {@link IllegalArgumentException}, igual que el resto de los
 * comandos de Application; Presentation las traduce a VALIDACION.
 */
public final class DiscoveryData {
    private DiscoveryData() {}

    /** Orden de los resultados; la semántica de cada uno está en {@code docs/decision-busqueda.md}. */
    public enum SearchOrder { RELEVANCE, BEST_RATING, LOWEST_PRICE, MOST_RECENT }

    /** Página solicitada; el número de página empieza en cero. */
    public record PageRequest(int page, int size) {
        public static final int MAX_SIZE = 100;
        public static final int DEFAULT_SIZE = 20;
        public static final PageRequest FIRST = new PageRequest(0, DEFAULT_SIZE);

        public PageRequest {
            if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
            if (size < 1 || size > MAX_SIZE) {
                throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_SIZE);
            }
            if ((long) page * size > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La página solicitada excede el máximo permitido");
            }
        }

        /** Índice del primer elemento de la página. */
        public int offset() {
            return page * size;
        }
    }

    /** Rango cerrado de precio base en SkillCoins; un límite nulo significa "sin límite". */
    public record PriceRange(Integer minimum, Integer maximum) {
        public static final PriceRange ANY = new PriceRange(null, null);

        public PriceRange {
            if (minimum != null && minimum < 0) throw new IllegalArgumentException("El precio mínimo no puede ser negativo");
            if (maximum != null && maximum < 0) throw new IllegalArgumentException("El precio máximo no puede ser negativo");
            if (minimum != null && maximum != null && minimum > maximum) {
                throw new IllegalArgumentException("El rango de precios está invertido");
            }
        }

        public boolean contains(int amount) {
            return (minimum == null || amount >= minimum) && (maximum == null || amount <= maximum);
        }
    }

    /**
     * Criterios de búsqueda del catálogo público. Un texto, categoría o habilidad vacíos y un
     * filtro nulo significan "sin filtro". Semántica completa en {@code docs/decision-busqueda.md}.
     */
    public record SearchQuery(String text, String category, Modality modality, String skill,
                              PriceRange price, Double minimumReputation,
                              SearchOrder order, PageRequest page) {
        public static final int MAX_TEXT_LENGTH = 100;

        public SearchQuery {
            text = text == null ? "" : text.trim();
            category = category == null ? null : category.trim();
            skill = skill == null ? null : skill.trim();
            price = price == null ? PriceRange.ANY : price;
            order = order == null ? SearchOrder.RELEVANCE : order;
            page = page == null ? PageRequest.FIRST : page;
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("El texto de búsqueda admite hasta " + MAX_TEXT_LENGTH + " caracteres");
            }
            if (minimumReputation != null && (!Double.isFinite(minimumReputation)
                    || minimumReputation < 0 || minimumReputation > 5)) {
                throw new IllegalArgumentException("La reputación mínima debe estar entre 0 y 5");
            }
        }
    }
}
