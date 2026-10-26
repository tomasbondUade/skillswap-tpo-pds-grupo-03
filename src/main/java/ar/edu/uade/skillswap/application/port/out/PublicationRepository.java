package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.domain.model.Publication;
import java.util.List;
import java.util.Optional;

/** Persistence and query contract for Publication aggregates. */
public interface PublicationRepository {
    Optional<Publication> findById(String publicationId);
    Optional<Publication> findByIdForUpdate(String publicationId);
    List<Publication> findByAuthorUserId(String authorUserId);
    List<Publication> findPublished();
    PageResult<Publication> searchPublished(SearchQuery query);
    Publication save(Publication publication);
}
