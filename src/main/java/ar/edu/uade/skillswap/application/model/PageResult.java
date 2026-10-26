package ar.edu.uade.skillswap.application.model;

import java.util.List;

/** Paginated application result; contains read data, never persistence entities. */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {
    public PageResult {
        items = List.copyOf(items == null ? List.of() : items);
        if (page < 0 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("La paginación es inválida");
        }
        if (items.size() > size) throw new IllegalArgumentException("La página supera el tamaño solicitado");
    }

    public int totalPages() {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
