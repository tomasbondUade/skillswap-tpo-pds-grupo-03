package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory profile repository used to connect the current mock UI and application services. */
public final class InMemoryProfileRepository implements ProfileRepository {
    private final Map<String, Profile> profiles = new HashMap<>();

    @Override
    public Optional<Profile> findByUserId(String userId) {
        return Optional.ofNullable(profiles.get(userId));
    }

    @Override
    public Optional<Profile> findByUserIdForUpdate(String userId) {
        return findByUserId(userId);
    }

    @Override
    public Profile save(Profile profile) {
        profiles.put(profile.userId(), profile);
        return profile;
    }
}
