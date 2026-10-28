package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.domain.model.Wallet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryWalletRepository implements WalletRepository {
    private final Map<String, Wallet> wallets = new HashMap<>();

    @Override
    public Optional<Wallet> findByUserId(String userId) {
        return Optional.ofNullable(wallets.get(userId));
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(String userId) {
        return findByUserId(userId);
    }

    @Override
    public Wallet save(Wallet wallet) {
        wallets.put(wallet.userId(), wallet);
        return wallet;
    }
}