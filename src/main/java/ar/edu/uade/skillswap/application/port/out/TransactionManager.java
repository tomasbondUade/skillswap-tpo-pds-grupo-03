package ar.edu.uade.skillswap.application.port.out;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Puerto de transacciones de la aplicación.
 */
public interface TransactionManager {

    <T> T required(Supplier<T> work);

    /**
     * Ejecuta una acción después de confirmar la transacción.
     */
    default void afterCommit(Runnable action) {
        Objects.requireNonNull(action, "Falta la acción");
        action.run();
    }

    default void required(Runnable work) {
        Objects.requireNonNull(work, "Falta la operación");
        required(() -> {
            work.run();
            return null;
        });
    }
}
