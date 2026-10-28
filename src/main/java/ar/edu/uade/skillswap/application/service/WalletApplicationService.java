package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.WalletApplicationMapper;
import ar.edu.uade.skillswap.application.model.WalletData.WalletView;
import ar.edu.uade.skillswap.application.port.in.WalletUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import java.util.Objects;

/** Returns the authenticated user's wallet without exposing the domain aggregate. */
public final class WalletApplicationService implements WalletUseCases {
    private final WalletRepository wallets;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;

    public WalletApplicationService(WalletRepository wallets, CurrentUserProvider currentUser,
                                    TransactionManager transactions) {
        this.wallets = Objects.requireNonNull(wallets);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public WalletView viewMyWallet() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        return wallets.findByUserId(actor)
                .map(WalletApplicationMapper::toView)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                        "No existe una billetera para el usuario autenticado"));
    }

    @Override
    public WalletView creditDemo(int amountInSkillCoins) {
        String actor = UseCaseSupport.currentUserId(currentUser);
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            var wallet = wallets.findByUserIdForUpdate(actor)
                    .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                            "No existe una billetera para el usuario autenticado"));
            wallet.credit(amountInSkillCoins, "Créditos demo");
            return WalletApplicationMapper.toView(wallets.save(wallet));
        }));
    }
}
