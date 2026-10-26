package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.WalletData.WalletView;

/** Inbound read use case for the authenticated user's wallet. */
public interface WalletUseCases {
    WalletView viewMyWallet();
}
