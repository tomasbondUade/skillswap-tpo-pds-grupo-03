
package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.ProfileMapper;

import jakarta.persistence.LockModeType;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementación JPA del repositorio de perfiles.
 */
public final class JpaProfileRepository extends JpaRepositorySupport
        implements ProfileRepository {

    public JpaProfileRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public Optional<Profile> findByUserId(String userId) {
        Objects.requireNonNull(userId, "Falta el identificador del usuario");

        return query(em -> em.createQuery(
                        "SELECT p FROM ProfileEntity p "
                                + "WHERE p.userId = :userId",
                        ProfileEntity.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(ProfileMapper::toDomain));
    }

    @Override
    public Optional<Profile> findByUserIdForUpdate(String userId) {
        Objects.requireNonNull(userId, "Falta el identificador del usuario");

        return query(em -> em.createQuery(
                        "SELECT p FROM ProfileEntity p "
                                + "WHERE p.userId = :userId",
                        ProfileEntity.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .map(ProfileMapper::toDomain));
    }

    @Override
    public Profile save(Profile profile) {
        Objects.requireNonNull(profile, "Falta el perfil");

        ProfileEntity entity = ProfileMapper.toEntity(profile);

        return ProfileMapper.toDomain(
                entityManager().merge(entity));
    }
}
