package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import java.util.function.Supplier;

final class ImmediateTransactionManager implements TransactionManager {
    int operations;

    @Override
    public <T> T required(Supplier<T> work) {
        operations++;
        return work.get();
    }
}
