package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Profile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence contract for Profile aggregates. */
public interface ProfileRepository {
    Optional<Profile> findByUserId(String userId);
    Optional<Profile> findByUserIdForUpdate(String userId);

    /** Perfiles de varios usuarios en una única lectura (evita N+1); omite los usuarios sin perfil. */
    List<Profile> findByUserIds(Collection<String> userIds);

    Profile save(Profile profile);
}
