package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.Optional;

/** Persistence contract for Profile aggregates. */
public interface ProfileRepository {
    Optional<Profile> findByUserId(String userId);
    Optional<Profile> findByUserIdForUpdate(String userId);
    Profile save(Profile profile);
}
