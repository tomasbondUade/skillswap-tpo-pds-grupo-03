package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.model.Models.ErrorPresentacion;
import java.util.Objects;
import java.util.function.Consumer;

/** Stops a failed UI action and presents its error without running success feedback. */
public final class UiActionHandler {
    private final Consumer<ErrorPresentacion> showError;

    public UiActionHandler(Consumer<ErrorPresentacion> showError) {
        this.showError = Objects.requireNonNull(showError);
    }

    public void run(Runnable action) {
        try {
            action.run();
        } catch (PresentationException exception) {
            showError.accept(exception.error());
        }
    }
}
