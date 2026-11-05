package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * In-memory publication repository used as infrastructure seed for the application layer.
 * The search reads author profiles through the {@link ProfileRepository} port: it is the in-memory
 * equivalent of the join with the profiles table that the JPA query performs.
 */
public final class InMemoryPublicationRepository implements PublicationRepository {
    private final Map<String, Publication> publications = new HashMap<>();
    private final ProfileRepository profiles;

    public InMemoryPublicationRepository(ProfileRepository profiles) {
        this.profiles = Objects.requireNonNull(profiles, "Falta el repositorio de perfiles");
    }

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
        Objects.requireNonNull(query, "Faltan los criterios de búsqueda");
        List<Publication> published = findPublished();
        Map<String, Profile> authors = profiles.findByUserIds(published.stream()
                        .map(Publication::authorUserId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Profile::userId, Function.identity()));

        List<Publication> matches = published.stream()
                .filter(matches(query, authors))
                .sorted(orderFor(query, authors))
                .toList();

        int from = Math.min(query.page() * query.size(), matches.size());
        int to = Math.min(from + query.size(), matches.size());
        return new PageResult<>(matches.subList(from, to), query.page(), query.size(), matches.size());
    }

    @Override
    public Publication save(Publication publication) {
        publications.put(publication.id(), publication);
        return publication;
    }

    private static Predicate<Publication> matches(SearchQuery query, Map<String, Profile> authors) {
        String text = normalize(query.text());
        String skill = normalize(query.skill());
        Predicate<Publication> activeAuthor = item -> authors.containsKey(item.authorUserId())
                && authors.get(item.authorUserId()).active();
        Predicate<Publication> byText = item -> text.isBlank()
                || normalize(item.title()).contains(text)
                || normalize(item.description()).contains(text)
                || normalize(item.category()).contains(text)
                || item.tags().stream().anyMatch(tag -> normalize(tag).contains(text));
        Predicate<Publication> byCategory = item -> isBlank(query.category())
                || item.category().equalsIgnoreCase(query.category());
        Predicate<Publication> byModality = item -> query.modality() == null || item.modality() == query.modality();
        Predicate<Publication> bySkill = item -> skill.isBlank()
                || item.tags().stream().anyMatch(tag -> normalize(tag).equals(skill));
        Predicate<Publication> byPrice = item -> (query.minimumPrice() == null
                || item.basePrice().amount() >= query.minimumPrice())
                && (query.maximumPrice() == null || item.basePrice().amount() <= query.maximumPrice());
        Predicate<Publication> byReputation = item -> query.minimumReputation() == null
                || authors.get(item.authorUserId()).reputation() >= query.minimumReputation();
        return activeAuthor.and(byText).and(byCategory).and(byModality).and(bySkill).and(byPrice).and(byReputation);
    }

    private static Comparator<Publication> orderFor(SearchQuery query, Map<String, Profile> authors) {
        Comparator<Publication> byRequestsDesc = Comparator.comparingInt(Publication::requestCount).reversed();
        Comparator<Publication> byCreatedDesc = Comparator.comparing(Publication::createdAt).reversed();
        Comparator<Publication> primary = switch (query.order()) {
            case RELEVANCE -> byRequestsDesc.thenComparing(byCreatedDesc);
            case BEST_RATING -> Comparator.comparingDouble(
                            (Publication item) -> authors.get(item.authorUserId()).reputation()).reversed()
                    .thenComparing(Comparator.comparingInt(
                            (Publication item) -> authors.get(item.authorUserId()).ratingCount()).reversed())
                    .thenComparing(byCreatedDesc);
            case LOWEST_PRICE -> Comparator.comparingInt((Publication item) -> item.basePrice().amount())
                    .thenComparing(byCreatedDesc);
            case MOST_RECENT -> byCreatedDesc;
        };
        return primary.thenComparing(Publication::id);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Case- and accent-insensitive form, equivalent to MySQL utf8mb4_0900_ai_ci comparisons. */
    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
