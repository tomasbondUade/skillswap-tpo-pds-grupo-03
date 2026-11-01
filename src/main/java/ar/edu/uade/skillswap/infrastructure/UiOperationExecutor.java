package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.controllers.mapper.UiErrorMapper;
import ar.edu.uade.skillswap.model.Models.CodigoError;
import ar.edu.uade.skillswap.model.Models.ErrorPresentacion;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;

/** Infrastructure boundary: JPA/JDBC details never become UI messages. */
public final class UiOperationExecutor {
    private final UiErrorMapper errors = new UiErrorMapper();

    public <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (PresentationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PresentationException(toError(exception), exception);
        }
    }

    public void execute(Runnable operation) {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    private ErrorPresentacion toError(RuntimeException exception) {
        boolean persistenceFailure = false;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return new ErrorPresentacion(CodigoError.CONFLICTO,
                        "La información cambió. Actualizá la pantalla y volvé a intentar.");
            }
            if (cause instanceof PersistenceException || cause instanceof SQLException) {
                persistenceFailure = true;
            }
        }
        if (persistenceFailure) {
            return new ErrorPresentacion(CodigoError.PERSISTENCIA,
                    "No se pudo acceder a los datos. Volvé a intentar más tarde.");
        }
        return errors.toError(exception);
    }
}
