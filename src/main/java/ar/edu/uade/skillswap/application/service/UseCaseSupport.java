package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import java.util.function.Supplier;

/** Small shared boundary helpers; business rules remain in domain aggregates. */
final class UseCaseSupport {
    private UseCaseSupport() {}

    static String currentUserId(CurrentUserProvider provider) {
        String userId = provider.currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED,
                    "Se requiere un usuario autenticado");
        }
        return userId.trim();
    }

    static String requiredId(String id, String label) {
        if (id == null || id.isBlank()) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION,
                    "Falta el identificador de " + label);
        }
        return id.trim();
    }

    static <T> T translateDomainFailures(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (ApplicationException exception) {
            throw exception;
        } catch (SecurityException exception) {
            throw new ApplicationException(ApplicationErrorCode.FORBIDDEN,
                    "No tenés permiso para realizar esta operación", exception);
        } catch (IllegalStateException exception) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_STATE,
                    exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION,
                    exception.getMessage(), exception);
        }
    }

    static void translateDomainFailures(Runnable operation) {
        translateDomainFailures(() -> {
            operation.run();
            return null;
        });
    }
}
