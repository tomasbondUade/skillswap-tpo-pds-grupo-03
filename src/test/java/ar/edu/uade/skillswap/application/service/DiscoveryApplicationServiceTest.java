package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.RecommendationStrategy;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscoveryApplicationServiceTest {
    @Test
    void mapsPagedSearchResultsToApplicationViews() {
        Publication publication = publication("Java", "Tecnología");
        var repository = new StubPublications(List.of(publication));
        var service = new DiscoveryApplicationService(repository, profileRepository(Optional.empty()), () -> "user-1",
                strategyReturningFirst());

        var query = new SearchQuery("java", null, null, null, null, null, SearchOrder.RELEVANCE, 2, 5);
        PageResult<ar.edu.uade.skillswap.application.model.PublicationData.PublicationView> result = service.search(query);

        assertEquals(2, result.page());
        assertEquals(5, result.size());
        assertEquals(7, result.totalElements());
        assertEquals("Java", result.items().get(0).title());
    }

    @Test
    void usesInjectedRecommendationStrategyWithAuthenticatedProfileAndLimit() {
        Profile profile = Profile.register("user-1", "Ana", "", "UTC", Set.of("Java"), Set.of(), Set.of());
        Publication publication = publication("Java", "Tecnología");
        var repository = new StubPublications(List.of(publication));
        var profiles = new ProfileRepository() {
            @Override public Optional<Profile> findByUserId(String userId) { return Optional.of(profile); }
            @Override public Optional<Profile> findByUserIdForUpdate(String userId) { return Optional.of(profile); }
            @Override public Profile save(Profile value) { return value; }
        };
        var strategy = new CapturingStrategy();
        var service = new DiscoveryApplicationService(repository, profiles, () -> "user-1", strategy);

        var result = service.recommend(3);

        assertSame(profile, strategy.profile);
        assertEquals(3, strategy.limit);
        assertEquals("Java", result.get(0).title());
    }

    @Test
    void listsDistinctPublishedCategoriesInStableOrder() {
        var repository = new StubPublications(List.of(
                publication("Java", "Tecnología"), publication("UX", "Diseño"), publication("SQL", "Tecnología")));
        var service = new DiscoveryApplicationService(repository, profileRepository(Optional.empty()), () -> "user-1",
                strategyReturningFirst());

        assertEquals(List.of("Diseño", "Tecnología"), service.categories());
    }

    private static RecommendationStrategy strategyReturningFirst() {
        return new RecommendationStrategy() {
            @Override public List<Publication> recommend(Profile profile, List<Publication> candidates, int limit) {
                return candidates.stream().limit(limit).toList();
            }
            @Override public String name() { return "test"; }
        };
    }

    private static ProfileRepository profileRepository(Optional<Profile> profile) {
        return new ProfileRepository() {
            @Override public Optional<Profile> findByUserId(String userId) { return profile; }
            @Override public Optional<Profile> findByUserIdForUpdate(String userId) { return profile; }
            @Override public Profile save(Profile value) { return value; }
        };
    }

    private static Publication publication(String title, String category) {
        return new Publication("pub-" + title, "provider-1", title, "Descripción", category, Modality.REMOTE,
                60, 2, new SkillCoins(10), Set.of(), List.of(), PublicationStates.published(),
                LocalDateTime.now(), 0, null, 0, List.of());
    }

    private static final class CapturingStrategy implements RecommendationStrategy {
        private Profile profile;
        private int limit;

        @Override public List<Publication> recommend(Profile profile, List<Publication> candidates, int limit) {
            this.profile = profile;
            this.limit = limit;
            return candidates.stream().limit(limit).toList();
        }
        @Override public String name() { return "captura"; }
    }

    private static final class StubPublications implements PublicationRepository {
        private final List<Publication> items;
        private StubPublications(List<Publication> items) { this.items = items; }

        @Override public Optional<Publication> findById(String id) { return Optional.empty(); }
        @Override public Optional<Publication> findByIdForUpdate(String id) { return Optional.empty(); }
        @Override public List<Publication> findByAuthorUserId(String userId) { return List.of(); }
        @Override public List<Publication> findPublished() { return items; }
        @Override public PageResult<Publication> searchPublished(SearchQuery query) {
            return new PageResult<>(items.subList(0, 1), query.page(), query.size(), 7);
        }
        @Override public Publication save(Publication publication) { return publication; }
    }
}
