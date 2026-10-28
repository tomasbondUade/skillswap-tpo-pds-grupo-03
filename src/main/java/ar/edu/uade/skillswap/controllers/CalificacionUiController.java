package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.model.RatingData.*;
import ar.edu.uade.skillswap.application.port.in.RatingUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.model.Models;
import java.util.Objects;

/** Convierte y delega la calificación; las reglas de puntaje, unicidad y participantes son del caso de uso. */
public final class CalificacionUiController implements UiControllers.CalificacionController {
    private final RatingUseCases ratings;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;

    public CalificacionUiController(RatingUseCases ratings, CurrentUserProvider identity,
                                   ControllerExecutor operations) {
        this.ratings = Objects.requireNonNull(ratings);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    @Override public Models.Calificacion calificar(String userId, String contratacionId,
                                                   int puntaje, String comentario) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            ControllerUser.required(contratacionId, "Falta la contratación");
            if (puntaje < 1 || puntaje > 5) throw new IllegalArgumentException("El puntaje debe estar entre 1 y 5");
            RatingView result = ratings.submit(new SubmitCommand(contratacionId.trim(), puntaje, comentario));
            return new Models.Calificacion(result.id(), result.bookingId(), result.score(),
                    result.comment(), result.createdAt());
        });
    }
}
