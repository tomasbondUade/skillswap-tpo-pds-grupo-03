package ar.edu.uade.skillswap.controllers.mapper;

import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.model.Models.CodigoError;
import ar.edu.uade.skillswap.model.Models.ErrorPresentacion;

/** Maps application failures to a stable UI contract. Persistence is translated in infrastructure. */
public final class UiErrorMapper {
    public ErrorPresentacion toError(RuntimeException exception) {
        if (exception instanceof ApplicationException application) {
            return switch (application.code()) {
                case VALIDATION -> error(CodigoError.VALIDACION, application,
                        "Revisá los datos ingresados.");
                case UNAUTHENTICATED -> new ErrorPresentacion(CodigoError.NO_AUTENTICADO,
                        "Se requiere un usuario autenticado.");
                case NOT_FOUND -> error(CodigoError.NO_ENCONTRADO, application,
                        "No se encontró la información solicitada.");
                case FORBIDDEN -> new ErrorPresentacion(CodigoError.PERMISOS,
                        "No tenés permiso para realizar esta operación.");
                case INVALID_STATE -> error(CodigoError.ESTADO, application,
                        "La operación no está permitida en el estado actual.");
                case CONFLICT -> error(CodigoError.CONFLICTO, application,
                        "La operación entra en conflicto con la información actual.");
            };
        }
        if (exception instanceof SecurityException) {
            return new ErrorPresentacion(CodigoError.PERMISOS,
                    "No tenés permiso para realizar esta operación.");
        }
        if (exception instanceof IllegalArgumentException) {
            return error(CodigoError.VALIDACION, exception, "Revisá los datos ingresados.");
        }
        if (exception instanceof IllegalStateException) {
            return error(CodigoError.ESTADO, exception,
                    "La operación no está permitida en el estado actual.");
        }
        return new ErrorPresentacion(CodigoError.INESPERADO,
                "No se pudo completar la operación. Volvé a intentar.");
    }

    private ErrorPresentacion error(CodigoError code, RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        return new ErrorPresentacion(code, message == null || message.isBlank() ? fallback : message);
    }
}
