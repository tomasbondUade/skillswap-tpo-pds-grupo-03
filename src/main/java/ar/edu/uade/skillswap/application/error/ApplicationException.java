package ar.edu.uade.skillswap.application.error;

import java.util.Objects;

/** A framework-free failure contract for application use cases. */
public final class ApplicationException extends RuntimeException {
    private final ApplicationErrorCode code;

    public ApplicationException(ApplicationErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "Falta el código del error");
    }

    public ApplicationException(ApplicationErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "Falta el código del error");
    }

    public ApplicationErrorCode code() {
        return code;
    }
}
