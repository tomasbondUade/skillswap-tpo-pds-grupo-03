package ar.edu.uade.skillswap.application.model;

import java.time.LocalDateTime;
import java.util.List;

public final class WalletData {
    private WalletData() {}

    public record MovementView(String id, String concept, int amountInSkillCoins,
                               int resultingBalanceInSkillCoins, LocalDateTime occurredAt) {}

    public record WalletView(String userId, int balanceInSkillCoins, List<MovementView> movements) {
        public WalletView { movements = List.copyOf(movements == null ? List.of() : movements); }
    }
}
