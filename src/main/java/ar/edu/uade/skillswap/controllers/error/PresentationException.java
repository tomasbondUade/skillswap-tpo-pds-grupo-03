package ar.edu.uade.skillswap.controllers.error;

import ar.edu.uade.skillswap.model.Models.ErrorPresentacion;
import java.util.Objects;

/** Uniform failure exposed by the real controllers without changing their success results. */
public final class PresentationException extends RuntimeException {
    private final ErrorPresentacion error;

    public PresentationException(ErrorPresentacion error, Throwable cause) {
        super(Objects.requireNonNull(error, "Falta el error de presentación").mensaje(), cause);
        this.error = error;
    }

    public ErrorPresentacion error() {
        return error;
    }
}
