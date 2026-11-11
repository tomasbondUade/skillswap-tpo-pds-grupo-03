package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.RecommendationStrategy;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DiscoveryApplicationServiceTest {
    @Test
    void delegaLaBusquedaSinModificarLosCriteriosYConservaLaPaginacion() {
        var publications = new StubPublications(List.of(), new PageResult<>(List.of(publication("p1", "ana")), 2, 5, 11));
        var service = service(publications, new StubProfiles(List.of(profile("ana", 0, 0))));
        var query = new SearchQuery("java", "Tecnología", Modality.REMOTE, "Java", 10, 200,
                4.0, SearchOrder.BEST_RATING, 2, 5);

        PageResult<PublicationView> result = service.search(query);

        assertSame(query, publications.receivedQuery);
        assertEquals(2, result.page());
        assertEquals(5, result.size());
        assertEquals(11, result.totalElements());
        assertEquals("pub-p1", result.items().get(0).id());
    }

    @Test
    void completaCadaResultadoConLosDatosDelAutorEnUnaSolaLecturaDePerfiles() {
        var slot = new AvailabilitySlot("slot-1", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2), 2, 1);
        var page = new PageResult<>(List.of(publication("p1", "ana", slot), publication("p2", "ana"),
                publication("p3", "beto")), 0, 20, 3);
        var profiles = new StubProfiles(List.of(profile("ana", 4.5, 2), profile("beto", 3.0, 1)));
        var service = service(new StubPublications(List.of(), page), profiles);

        List<PublicationView> items = service.search(anyQuery()).items();

        assertEquals(1, profiles.batchCalls);
        assertEquals(Set.of("ana", "beto"), Set.copyOf(profiles.requestedUserIds));
        assertEquals("Nombre ana", items.get(0).author().displayName());
        assertEquals(4.5, items.get(0).author().reputation());
        assertEquals(2, items.get(0).author().ratingCount());
        assertEquals("slot-1", items.get(0).availability().get(0).id());
        assertEquals(1, items.get(0).availability().get(0).reserved());
        assertEquals("Nombre beto", items.get(2).author().displayName());
        assertEquals(3.0, items.get(2).author().reputation());
    }

    @Test
    void unAutorSinPerfilEsUnaInconsistenciaDeDatos() {
        var page = new PageResult<>(List.of(publication("p1", "org-1")), 0, 20, 1);
        var service = service(new StubPublications(List.of(), page), new StubProfiles(List.of()));

        var error = assertThrows(IllegalStateException.class, () -> service.search(anyQuery()));

        assertEquals("La publicación pub-p1 no tiene perfil de autor", error.getMessage());
    }

    @Test
    void rechazaCriteriosNulosConErrorDeValidacion() {
        var publications = new StubPublications(List.of(), emptyPage());
        var service = service(publications, new StubProfiles(List.of()));

        var error = assertThrows(ApplicationException.class, () -> service.search(null));

        assertEquals(ApplicationErrorCode.VALIDATION, error.code());
        assertNull(publications.receivedQuery);
    }

    @Test
    void propagaLasFallasDelRepositorioSinOcultarlas() {
        var publications = new StubPublications(List.of(), null) {
            @Override public PageResult<Publication> searchPublished(SearchQuery query) {
                throw new IllegalStateException("Base no disponible");
            }
        };
        var service = service(publications, new StubProfiles(List.of()));

        var error = assertThrows(IllegalStateException.class, () -> service.search(anyQuery()));

        assertEquals("Base no disponible", error.getMessage());
    }

    @Test
    void laBusquedaNoRequiereUsuarioAutenticado() {
        var page = new PageResult<>(List.of(publication("p1", "ana")), 0, 20, 1);
        var service = new DiscoveryApplicationService(new StubPublications(List.of(), page),
                new StubProfiles(List.of(profile("ana", 0, 0))), () -> null, strategyReturningFirst());

        assertEquals(1, service.search(anyQuery()).totalElements());
    }

    @Test
    void usesInjectedRecommendationStrategyWithAuthenticatedProfileAndLimit() {
        Profile profile = Profile.register("user-1", "Ana", "", "UTC", Set.of("Java"), Set.of(), Set.of());
        var repository = new StubPublications(List.of(publication("Java", "provider-1")), emptyPage());
        var strategy = new CapturingStrategy();
        var profiles = new StubProfiles(List.of(profile, profile("provider-1", 4.2, 3)));
        var service = new DiscoveryApplicationService(repository, profiles, () -> "user-1", strategy);

        List<PublicationView> result = service.recommend(3);

        assertSame(profile, strategy.profile);
        assertEquals(3, strategy.limit);
        assertEquals("Mentoría Java", result.get(0).title());
        assertEquals("Nombre provider-1", result.get(0).author().displayName());
        assertEquals(4.2, result.get(0).author().reputation());
        assertEquals(1, profiles.batchCalls);
    }

    @Test
    void listsDistinctPublishedCategoriesInStableOrder() {
        var repository = new StubPublications(List.of(
                publication("Java", "provider-1", "Tecnología"), publication("UX", "provider-1", "Diseño"),
                publication("SQL", "provider-1", "Tecnología")), emptyPage());
        var service = service(repository, new StubProfiles(List.of()));

        assertEquals(List.of("Diseño", "Tecnología"), service.categories());
    }

    private static DiscoveryApplicationService service(PublicationRepository publications, ProfileRepository profiles) {
        return new DiscoveryApplicationService(publications, profiles, () -> "user-1", strategyReturningFirst());
    }

    private static SearchQuery anyQuery() {
        return new SearchQuery(null, null, null, null, null, null, null, null, 0, 20);
    }

    private static PageResult<Publication> emptyPage() {
        return new PageResult<>(List.of(), 0, 20, 0);
    }

    private static RecommendationStrategy strategyReturningFirst() {
        return new RecommendationStrategy() {
            @Override public List<Publication> recommend(Profile profile, List<Publication> candidates, int limit) {
                return candidates.stream().limit(limit).toList();
            }
            @Override public String name() { return "test"; }
        };
    }

    private static Profile profile(String userId, double reputation, int ratingCount) {
        return new Profile(null, userId, ActorType.PROFESSIONAL, "Nombre " + userId, "", "UTC",
                Set.of(), Set.of(), Set.of(),
                reputation, ratingCount, true);
    }

    private static Publication publication(String id, String author, AvailabilitySlot... slots) {
        return publication(id, author, "Tecnología", slots);
    }

    private static Publication publication(String id, String author, String category, AvailabilitySlot... slots) {
        return new Publication("pub-" + id, author, PublicationType.MENTORSHIP, "Mentoría " + id, "Descripción",
                category, Modality.REMOTE, 60, 2, new SkillCoins(10), Set.of(), List.of(slots),
                PublicationStates.published(),
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

    private static class StubPublications implements PublicationRepository {
        private final List<Publication> published;
        private final PageResult<Publication> searchResult;
        private SearchQuery receivedQuery;

        private StubPublications(List<Publication> published, PageResult<Publication> searchResult) {
            this.published = published;
            this.searchResult = searchResult;
        }

        @Override public Optional<Publication> findById(String id) { return Optional.empty(); }
        @Override public Optional<Publication> findByIdForUpdate(String id) { return Optional.empty(); }
        @Override public List<Publication> findByAuthorUserId(String userId) { return List.of(); }
        @Override public List<Publication> findPublished() { return published; }
        @Override public PageResult<Publication> searchPublished(SearchQuery query) {
            receivedQuery = query;
            return searchResult;
        }
        @Override public Publication save(Publication publication) { return publication; }
    }

    /** Registra las lecturas en lote para verificar que no se consulta un perfil por resultado. */
    private static final class StubProfiles implements ProfileRepository {
        private final Map<String, Profile> byUserId;
        private int batchCalls;
        private final List<String> requestedUserIds = new ArrayList<>();

        private StubProfiles(List<Profile> profiles) {
            this.byUserId = profiles.stream().collect(Collectors.toMap(Profile::userId, Function.identity()));
        }

        @Override public Optional<Profile> findByUserId(String userId) { return Optional.ofNullable(byUserId.get(userId)); }
        @Override public Optional<Profile> findByUserIdForUpdate(String userId) { return findByUserId(userId); }
        @Override public List<Profile> findByUserIds(Collection<String> userIds) {
            batchCalls++;
            requestedUserIds.addAll(userIds);
            return userIds.stream().map(byUserId::get).filter(profile -> profile != null).toList();
        }
        @Override public Profile save(Profile value) { return value; }
    }
}
