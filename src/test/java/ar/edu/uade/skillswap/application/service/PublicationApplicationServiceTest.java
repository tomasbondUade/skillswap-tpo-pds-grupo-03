package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityInput;
import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.EditDraftCommand;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PublicationApplicationServiceTest {
    @Test
    void createsEditsAndPublishesDraftThroughDomainAggregate() {
        var publications = new InMemoryPublications();
        var service = new PublicationApplicationService(publications, profilesOf("author-1"), () -> "author-1",
                new ImmediateTransactionManager());

        var created = service.createDraft(command("Java para principiantes"));
        assertEquals("author-1", created.authorUserId());
        assertEquals("Nombre author-1", created.author().displayName());
        assertEquals(PublicationStatus.DRAFT, created.status());
        assertEquals(1, created.availability().size());
        assertEquals("slot-1", created.availability().get(0).id());

        var edited = service.editDraft(new EditDraftCommand(created.id(), "Java desde cero", "Práctica guiada",
                "Tecnología", Modality.REMOTE, 60, 2, Set.of("Java", "Backend")));
        assertEquals("Java desde cero", edited.title());
        assertEquals(Set.of("Java", "Backend"), edited.tags());

        var published = service.changeStatus(new ChangeStatusCommand(created.id(), PublicationStatus.PUBLISHED, null));
        assertEquals(PublicationStatus.PUBLISHED, published.status());
        assertEquals(1, service.listMyPublications().size());
        assertEquals("author-1", service.listMyPublications().get(0).author().userId());
    }

    @Test
    void noPermiteCrearUnBorradorSinPerfilDeAutor() {
        var publications = new InMemoryPublications();
        var service = new PublicationApplicationService(publications, profilesOf(), () -> "author-1",
                new ImmediateTransactionManager());

        var error = assertThrows(ApplicationException.class, () -> service.createDraft(command("Java")));

        assertEquals(ApplicationErrorCode.NOT_FOUND, error.code());
        assertEquals(0, publications.findByAuthorUserId("author-1").size());
    }

    @Test
    void listarPropiasSinPublicacionesNoRequierePerfil() {
        var service = new PublicationApplicationService(new InMemoryPublications(), profilesOf(), () -> "author-1",
                new ImmediateTransactionManager());

        assertEquals(List.of(), service.listMyPublications());
    }

    @Test
    void translatesOwnershipAndInvalidTransitionFailures() {
        var publications = new InMemoryPublications();
        Publication existing = Publication.builder("another-author")
                .title("Java")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(10))
                .availability(List.of(slot()))
                .build();
        publications.save(existing);
        var service = new PublicationApplicationService(publications, profilesOf("user-1", "another-author"), () -> "user-1",
                new ImmediateTransactionManager());

        var forbidden = assertThrows(ApplicationException.class, () -> service.editDraft(new EditDraftCommand(
                existing.id(), "Java", "Mentoría", "Tecnología", Modality.REMOTE, 60, 2, Set.of())));
        assertEquals(ApplicationErrorCode.FORBIDDEN, forbidden.code());

        Publication ownDraft = Publication.builder("user-1")
                .title("Java")
                .description("Mentoría")
                .category("Tecnología")
                .modality(Modality.REMOTE)
                .durationMinutes(60)
                .capacity(2)
                .basePrice(new SkillCoins(10))
                .availability(List.of(slot()))
                .build();
        publications.save(ownDraft);
        var invalidState = assertThrows(ApplicationException.class, () -> service.changeStatus(
                new ChangeStatusCommand(ownDraft.id(), PublicationStatus.CLOSED, null)));
        assertEquals(ApplicationErrorCode.INVALID_STATE, invalidState.code());
    }

    private static ProfileRepository profilesOf(String... userIds) {
        Map<String, Profile> byUser = new HashMap<>();
        for (String userId : userIds) {
            byUser.put(userId, Profile.register(userId, "Nombre " + userId, "", "UTC", Set.of(), Set.of(), Set.of()));
        }
        return new ProfileRepository() {
            @Override public Optional<Profile> findByUserId(String userId) { return Optional.ofNullable(byUser.get(userId)); }
            @Override public Optional<Profile> findByUserIdForUpdate(String userId) { return findByUserId(userId); }
            @Override public List<Profile> findByUserIds(Collection<String> ids) {
                return ids.stream().map(byUser::get).filter(Objects::nonNull).toList();
            }
            @Override public Profile save(Profile profile) { return profile; }
        };
    }

    private static CreateDraftCommand command(String title) {
        return new CreateDraftCommand(title, "Sesión práctica", "Tecnología", Modality.REMOTE,
                60, 2, 20, Set.of("Java"), List.of(slotInput()));
    }

    private static AvailabilityInput slotInput() {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        return new AvailabilityInput("slot-1", startsAt, startsAt.plusHours(1), 2);
    }

    private static AvailabilitySlot slot() {
        AvailabilityInput input = slotInput();
        return new AvailabilitySlot(input.id(), input.startsAt(), input.endsAt(), input.capacity(), 0);
    }

    private static final class InMemoryPublications implements PublicationRepository {
        private final List<Publication> items = new ArrayList<>();

        @Override public Optional<Publication> findById(String publicationId) {
            return items.stream().filter(item -> item.id().equals(publicationId)).findFirst();
        }
        @Override public Optional<Publication> findByIdForUpdate(String publicationId) { return findById(publicationId); }
        @Override public List<Publication> findByAuthorUserId(String authorUserId) {
            return items.stream().filter(item -> item.authorUserId().equals(authorUserId)).toList();
        }
        @Override public List<Publication> findPublished() {
            return items.stream().filter(item -> item.status() == PublicationStatus.PUBLISHED).toList();
        }
        @Override public PageResult<Publication> searchPublished(SearchQuery query) {
            return new PageResult<>(List.of(), query.page().page(), query.page().size(), 0);
        }
        @Override public Publication save(Publication publication) {
            findById(publication.id()).ifPresent(items::remove);
            items.add(publication);
            return publication;
        }
    }
}
