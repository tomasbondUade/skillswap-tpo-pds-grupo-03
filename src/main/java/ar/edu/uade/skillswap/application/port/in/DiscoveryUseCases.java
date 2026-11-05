package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import java.util.List;

/** Inbound port for public search and personalized discovery. */
public interface DiscoveryUseCases {
    /**
     * UC-16: busca en el catálogo público con filtros, orden y paginación.
     * No requiere usuario autenticado porque no modifica información.
     *
     * @param query criterios ya validados (ver {@link SearchQuery})
     * @return página de publicaciones (con los datos públicos de su autor); una página fuera de rango
     *         devuelve una lista vacía con el total
     * @throws ApplicationException VALIDATION si faltan los criterios
     */
    PageResult<PublicationView> search(SearchQuery query);
    List<PublicationView> recommend(int limit);
    List<String> categories();
    ar.edu.uade.skillswap.application.model.DiscoveryData.AuthorView author(String userId);
}
