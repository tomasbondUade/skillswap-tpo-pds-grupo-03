package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** In-memory publication repository used as infrastructure seed for the application layer. */
public final class InMemoryPublicationRepository implements PublicationRepository {
    private final Map<String, Publication> publications = new HashMap<>();

    @Override
    public Optional<Publication> findById(String publicationId) {
        return Optional.ofNullable(publications.get(publicationId));
    }

    @Override
    public Optional<Publication> findByIdForUpdate(String publicationId) {
        return findById(publicationId);
    }

    @Override
    public List<Publication> findByAuthorUserId(String authorUserId) {
        return publications.values().stream()
                .filter(item -> item.authorUserId().equals(authorUserId))
                .toList();
    }

    @Override
    public List<Publication> findPublished() {
        return publications.values().stream()
                .filter(item -> item.status() == PublicationStatus.PUBLISHED)
                .toList();
    }

    @Override
    public PageResult<Publication> searchPublished(SearchQuery query) {
        Stream<Publication> stream = publications.values().stream()
                .filter(item -> item.status() == PublicationStatus.PUBLISHED);

        String text = query.text() == null ? "" : query.text().toLowerCase();
        if (!text.isBlank()) {
            stream = stream.filter(item -> item.title().toLowerCase().contains(text)
                    || item.description().toLowerCase().contains(text)
                    || item.tags().stream().anyMatch(tag -> tag.toLowerCase().contains(text))
                    || item.category().toLowerCase().contains(text));
        }
        if (query.category() != null && !query.category().isBlank()) {
            stream = stream.filter(item -> item.category().equalsIgnoreCase(query.category()));
        }
        if (query.modality() != null) {
            stream = stream.filter(item -> item.modality() == query.modality());
        }
        if (query.minimumPrice() != null) {
            stream = stream.filter(item -> item.basePrice().amount() >= query.minimumPrice());
        }
        if (query.maximumPrice() != null) {
            stream = stream.filter(item -> item.basePrice().amount() <= query.maximumPrice());
        }

        List<Publication> items = stream.toList();
        List<Publication> ordered = switch (query.order()) {
            case BEST_RATING -> items.stream().sorted(Comparator.comparingInt(Publication::requestCount).reversed()).toList();
            case LOWEST_PRICE -> items.stream().sorted(Comparator.comparingInt(item -> item.basePrice().amount())).toList();
            case MOST_RECENT -> items.stream().sorted(Comparator.comparing(Publication::createdAt).reversed()).toList();
            case RELEVANCE -> items.stream().sorted(Comparator.comparingInt(Publication::requestCount).reversed()).toList();
        };

        int page = query.page();
        int size = query.size();
        int fromIndex = Math.min(page * size, ordered.size());
        int toIndex = Math.min(fromIndex + size, ordered.size());
        List<Publication> pageItems = ordered.subList(fromIndex, toIndex);
        return new PageResult<>(pageItems, page, size, ordered.size());
    }

    @Override
    public Publication save(Publication publication) {
        publications.put(publication.id(), publication);
        return publication;
    }
}
