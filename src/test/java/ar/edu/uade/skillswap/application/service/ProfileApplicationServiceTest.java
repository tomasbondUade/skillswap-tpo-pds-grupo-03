package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.ProfileData.RegisterCommand;
import ar.edu.uade.skillswap.application.model.ProfileData.UpdateCommand;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfileApplicationServiceTest {
    @Test
    void registersAndMapsProfileForAuthenticatedUser() {
        var profiles = new InMemoryProfiles();
        var transactions = new ImmediateTransactionManager();
        var service = new ProfileApplicationService(profiles, () -> "  user-1  ", transactions);

        var result = service.register(new RegisterCommand("Ana", "Bio", "America/Argentina/Buenos_Aires",
                List.of("Java"), List.of("Mentoría"), Set.of(), List.of(), ActorType.PROFESSIONAL));

        assertEquals("user-1", result.userId());
        assertEquals(ActorType.PROFESSIONAL, result.actorType());
        assertEquals("Ana", result.displayName());
        assertEquals(Set.of("Java"), Set.copyOf(result.skills()));
        assertTrue(result.active());
        assertEquals(1, transactions.operations);
    }

    @Test
    void registersOrganizationWithItsActorType() {
        var service = new ProfileApplicationService(new InMemoryProfiles(), () -> "org-1",
                new ImmediateTransactionManager());

        var result = service.register(new RegisterCommand("Acme", "Empresa", "UTC", List.of(), List.of(),
                Set.of(), List.of(), ActorType.ORGANIZATION));

        assertEquals(ActorType.ORGANIZATION, result.actorType());
        assertEquals(ActorType.ORGANIZATION, service.viewMyProfile().actorType());
        assertThrows(IllegalArgumentException.class, () -> new RegisterCommand("Acme", "", "UTC",
                List.of(), List.of(), Set.of(), List.of(), null));
    }

    @Test
    void rejectsDuplicateProfileAndUnauthenticatedRequestsWithStableErrors() {
        var profiles = new InMemoryProfiles();
        profiles.save(Profile.register("user-1", ActorType.PROFESSIONAL, "Ana", "", "UTC", Set.of(),
                Set.of(), Set.of()));
        var service = new ProfileApplicationService(profiles, () -> "user-1", new ImmediateTransactionManager());

        var duplicate = assertThrows(ApplicationException.class, () -> service.register(
                new RegisterCommand("Ana", "", "UTC", List.of(), List.of(), Set.of(), List.of(),
                        ActorType.PROFESSIONAL)));
        assertEquals(ApplicationErrorCode.CONFLICT, duplicate.code());

        var unauthenticated = new ProfileApplicationService(profiles, () -> " ", new ImmediateTransactionManager());
        var error = assertThrows(ApplicationException.class, unauthenticated::viewMyProfile);
        assertEquals(ApplicationErrorCode.UNAUTHENTICATED, error.code());
    }

    @Test
    void updateAndDeactivateDelegateChangesToProfile() {
        var profiles = new InMemoryProfiles();
        profiles.save(Profile.register("user-1", ActorType.PROFESSIONAL, "Ana", "Bio", "UTC",
                Set.of("Java"), Set.of(), Set.of()));
        var service = new ProfileApplicationService(profiles, () -> "user-1", new ImmediateTransactionManager());

        var updated = service.update(new UpdateCommand("Ana B.", "Nueva bio", "UTC-3",
                List.of("Java", "SQL"), List.of("Docencia")));
        assertEquals("Ana B.", updated.displayName());
        assertEquals("UTC-3", updated.timeZone());
        assertEquals(Set.of("Java", "SQL"), Set.copyOf(updated.skills()));

        service.deactivateMyProfile();
        assertFalse(service.viewMyProfile().active());
        var error = assertThrows(ApplicationException.class, () -> service.update(
                new UpdateCommand("Ana", "", "UTC", List.of(), List.of())));
        assertEquals(ApplicationErrorCode.INVALID_STATE, error.code());

        service.activateMyProfile();
        assertTrue(service.viewMyProfile().active());
    }

    private static final class InMemoryProfiles implements ProfileRepository {
        private final Map<String, Profile> byUser = new HashMap<>();

        @Override public Optional<Profile> findByUserId(String userId) { return Optional.ofNullable(byUser.get(userId)); }
        @Override public Optional<Profile> findByUserIdForUpdate(String userId) { return findByUserId(userId); }
        @Override public List<Profile> findByUserIds(Collection<String> userIds) {
            return userIds.stream().map(byUser::get).filter(Objects::nonNull).toList();
        }
        @Override public Profile save(Profile profile) { byUser.put(profile.userId(), profile); return profile; }
    }
}
