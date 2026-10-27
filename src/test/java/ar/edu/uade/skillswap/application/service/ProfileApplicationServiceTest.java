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
import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.HashMap;
import java.util.Map;
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
                java.util.List.of("Java"), java.util.List.of("Mentoría"), Set.of(), java.util.List.of()));

        assertEquals("user-1", result.userId());
        assertEquals("Ana", result.displayName());
        assertEquals(Set.of("Java"), Set.copyOf(result.skills()));
        assertTrue(result.active());
        assertEquals(1, transactions.operations);
    }

    @Test
    void rejectsDuplicateProfileAndUnauthenticatedRequestsWithStableErrors() {
        var profiles = new InMemoryProfiles();
        profiles.save(Profile.register("user-1", "Ana", "", "UTC", Set.of(), Set.of(), Set.of()));
        var service = new ProfileApplicationService(profiles, () -> "user-1", new ImmediateTransactionManager());

        var duplicate = assertThrows(ApplicationException.class, () -> service.register(
                new RegisterCommand("Ana", "", "UTC", java.util.List.of(), java.util.List.of(), Set.of(), java.util.List.of())));
        assertEquals(ApplicationErrorCode.CONFLICT, duplicate.code());

        var unauthenticated = new ProfileApplicationService(profiles, () -> " ", new ImmediateTransactionManager());
        var error = assertThrows(ApplicationException.class, unauthenticated::viewMyProfile);
        assertEquals(ApplicationErrorCode.UNAUTHENTICATED, error.code());
    }

    @Test
    void updateAndDeactivateDelegateChangesToProfile() {
        var profiles = new InMemoryProfiles();
        profiles.save(Profile.register("user-1", "Ana", "Bio", "UTC", Set.of("Java"), Set.of(), Set.of()));
        var service = new ProfileApplicationService(profiles, () -> "user-1", new ImmediateTransactionManager());

        var updated = service.update(new UpdateCommand("Ana B.", "Nueva bio", "UTC-3",
                java.util.List.of("Java", "SQL"), java.util.List.of("Docencia")));
        assertEquals("Ana B.", updated.displayName());
        assertEquals("UTC-3", updated.timeZone());
        assertEquals(Set.of("Java", "SQL"), Set.copyOf(updated.skills()));

        service.deactivateMyProfile();
        assertFalse(service.viewMyProfile().active());
        var error = assertThrows(ApplicationException.class, () -> service.update(
                new UpdateCommand("Ana", "", "UTC", java.util.List.of(), java.util.List.of())));
        assertEquals(ApplicationErrorCode.INVALID_STATE, error.code());
    }

    private static final class InMemoryProfiles implements ProfileRepository {
        private final Map<String, Profile> byUser = new HashMap<>();

        @Override public Optional<Profile> findByUserId(String userId) { return Optional.ofNullable(byUser.get(userId)); }
        @Override public Optional<Profile> findByUserIdForUpdate(String userId) { return findByUserId(userId); }
        @Override public Profile save(Profile profile) { byUser.put(profile.userId(), profile); return profile; }
    }
}
