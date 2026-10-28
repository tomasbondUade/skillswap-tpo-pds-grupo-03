package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.Wallet;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class WalletApplicationServiceTest {
    @Test
    void returnsOnlyTheAuthenticatedUsersWalletAsApplicationData() {
        Wallet wallet = Wallet.open("user-1", 250);
        wallet.debit(30, "Reserva de mentoría");
        var service = new WalletApplicationService(new WalletRepository() {
            @Override public Optional<Wallet> findByUserId(String userId) {
                return "user-1".equals(userId) ? Optional.of(wallet) : Optional.empty();
            }
            @Override public Optional<Wallet> findByUserIdForUpdate(String userId) { return findByUserId(userId); }
            @Override public Wallet save(Wallet value) { return value; }
        }, () -> "user-1", new TransactionManager() {
            @Override public <T> T required(Supplier<T> work) { return work.get(); }
        });

        var view = service.viewMyWallet();

        assertEquals("user-1", view.userId());
        assertEquals(220, view.balanceInSkillCoins());
        assertEquals(2, view.movements().size());
        var lastMovement = view.movements().get(view.movements().size() - 1);
        assertEquals(-30, lastMovement.amountInSkillCoins());
        assertEquals(220, lastMovement.resultingBalanceInSkillCoins());
    }
}
