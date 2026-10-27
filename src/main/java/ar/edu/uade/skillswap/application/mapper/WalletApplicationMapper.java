package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.WalletData.MovementView;
import ar.edu.uade.skillswap.application.model.WalletData.WalletView;
import ar.edu.uade.skillswap.domain.model.Wallet;
import java.util.List;

/** Converts wallet aggregates to immutable application read models. */
public final class WalletApplicationMapper {
    private WalletApplicationMapper() {}

    public static WalletView toView(Wallet wallet) {
        List<MovementView> movements = wallet.movements().stream()
                .map(entry -> new MovementView(entry.id(), entry.concept(), entry.amount(),
                        entry.resultingBalance(), entry.occurredAt()))
                .toList();
        return new WalletView(wallet.userId(), wallet.balance().amount(), movements);
    }
}
