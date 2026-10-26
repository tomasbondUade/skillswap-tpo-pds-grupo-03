package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Wallet;
import java.util.Optional;

/** Persistence contract for wallets; lock-aware reads support atomic balance changes. */
public interface WalletRepository {
    Optional<Wallet> findByUserId(String userId);
    Optional<Wallet> findByUserIdForUpdate(String userId);
    Wallet save(Wallet wallet);
}
