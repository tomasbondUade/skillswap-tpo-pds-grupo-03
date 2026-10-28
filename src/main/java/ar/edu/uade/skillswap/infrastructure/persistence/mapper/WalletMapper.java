
package ar.edu.uade.skillswap.infrastructure.persistence.mapper;

import ar.edu.uade.skillswap.domain.model.LedgerEntry;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.WalletEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.WalletLedgerEntity;

import java.util.List;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static WalletEntity toEntity(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("La billetera no puede ser null");
        }

        WalletEntity entity = new WalletEntity();
        entity.setUserId(wallet.userId());
        entity.setBalance(wallet.balance().amount());

        for (LedgerEntry movement : wallet.movements()) {
            WalletLedgerEntity movementEntity = new WalletLedgerEntity();

            movementEntity.setId(movement.id());
            movementEntity.setConcept(movement.concept());
            movementEntity.setAmount(movement.amount());
            movementEntity.setResultingBalance(movement.resultingBalance());
            movementEntity.setOccurredAt(movement.occurredAt());

            entity.addMovement(movementEntity);
        }

        return entity;
    }

    public static Wallet toDomain(WalletEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad de billetera no puede ser null");
        }

        List<LedgerEntry> movements = entity.getMovements().stream()
                .map(movement -> new LedgerEntry(
                        movement.getId(),
                        movement.getConcept(),
                        movement.getAmount(),
                        movement.getResultingBalance(),
                        movement.getOccurredAt()
                ))
                .toList();

        return new Wallet(
                entity.getUserId(),
                new SkillCoins(entity.getBalance()),
                movements
        );
    }
}
