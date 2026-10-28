package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.domain.model.LedgerEntry;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.WalletEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.WalletLedgerEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.WalletMapper;

import jakarta.persistence.LockModeType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación JPA del repositorio de billeteras.
 */
public final class JpaWalletRepository extends JpaRepositorySupport
        implements WalletRepository {

    public JpaWalletRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public Optional<Wallet> findByUserId(String userId) {
        Objects.requireNonNull(
                userId, "Falta el identificador del titular");

        return query(em -> Optional.ofNullable(
                        em.find(WalletEntity.class, userId))
                .map(JpaWalletRepository::toDomain));
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(String userId) {
        Objects.requireNonNull(
                userId, "Falta el identificador del titular");

        return query(em -> em.createQuery(
                        "SELECT w FROM WalletEntity w "
                                + "WHERE w.userId = :userId",
                        WalletEntity.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .map(JpaWalletRepository::toDomain));
    }

    @Override
    public Wallet save(Wallet wallet) {
        Objects.requireNonNull(wallet, "Falta la billetera");

        WalletEntity existing = entityManager().find(
                WalletEntity.class, wallet.userId());

        if (existing == null) {
            WalletEntity entity = WalletMapper.toEntity(wallet);
            entityManager().persist(entity);
            return toDomain(entity);
        }

        existing.setBalance(wallet.balance().amount());

        synchronizeMovements(existing, wallet);

        return toDomain(existing);
    }

    /**
     * Conserva los movimientos persistidos y agrega únicamente los nuevos.
     * Un movimiento existente no puede modificarse ni reutilizar su ID.
     */
    private void synchronizeMovements(
            WalletEntity entity,
            Wallet wallet) {

        Map<String, WalletLedgerEntity> existingById = new HashMap<>();

        for (WalletLedgerEntity movement : entity.getMovements()) {
            existingById.put(movement.getId(), movement);
        }

        for (LedgerEntry movement : wallet.movements()) {
            WalletLedgerEntity existing =
                    existingById.get(movement.id());

            if (existing != null) {
                boolean sameMovement =
                        existing.getConcept().equals(movement.concept())
                        && existing.getAmount() == movement.amount()
                        && existing.getResultingBalance()
                                == movement.resultingBalance()
                        && existing.getOccurredAt()
                                .equals(movement.occurredAt());

                if (!sameMovement) {
                    throw new IllegalArgumentException(
                            "No se puede modificar un movimiento existente");
                }

                continue;
            }

            WalletLedgerEntity newMovement = new WalletLedgerEntity();
            newMovement.setId(movement.id());
            newMovement.setConcept(movement.concept());
            newMovement.setAmount(movement.amount());
            newMovement.setResultingBalance(
                    movement.resultingBalance());
            newMovement.setOccurredAt(movement.occurredAt());

            entity.addMovement(newMovement);
            existingById.put(movement.id(), newMovement);
        }

        entity.getMovements().sort(
                Comparator.comparing(WalletLedgerEntity::getOccurredAt)
                        .thenComparing(WalletLedgerEntity::getId));
    }

    private static Wallet toDomain(WalletEntity entity) {
        entity.getMovements().sort(
                Comparator.comparing(WalletLedgerEntity::getOccurredAt)
                        .thenComparing(WalletLedgerEntity::getId));

        return WalletMapper.toDomain(entity);
    }
}
