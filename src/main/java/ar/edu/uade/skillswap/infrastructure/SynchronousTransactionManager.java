package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import java.util.function.Supplier;

/** Simple infrastructure transaction manager for the current in-memory wiring. */
public final class SynchronousTransactionManager implements TransactionManager {
    @Override
    public <T> T required(Supplier<T> work) {
        return work.get();
    }
}
