package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;

/** Checks the caller against the active identity; resource ownership remains in the use case. */
final class ControllerUser {
    private ControllerUser() {}

    static void requireActive(CurrentUserProvider identity, String userId) {
        String active = identity.currentUserId();
        if (active == null || active.isBlank()) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED,
                    "Se requiere un usuario autenticado");
        }
        required(userId, "Falta el identificador del usuario");
        if (!active.trim().equals(userId.trim())) {
            throw new ApplicationException(ApplicationErrorCode.FORBIDDEN,
                    "El usuario indicado no coincide con la identidad activa");
        }
    }

    static void required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }
}
