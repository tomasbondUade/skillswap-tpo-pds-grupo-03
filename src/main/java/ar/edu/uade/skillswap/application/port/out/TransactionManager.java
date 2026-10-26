package ar.edu.uade.skillswap.application.port.out;

import java.util.Objects;
import java.util.function.Supplier;

/** Output port implemented by Infrastructure; atomically commits or rolls back one use case. */
public interface TransactionManager {
    <T> T required(Supplier<T> work);

    default void required(Runnable work) {
        Objects.requireNonNull(work, "Falta la operación");
        required(() -> { work.run(); return null; });
    }
}
