package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.model.Models.CodigoError;
import ar.edu.uade.skillswap.model.Models.ErrorPresentacion;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class UiActionHandlerTest {
    @Test
    void muestraElErrorYNoEjecutaElMensajeDeExito() {
        var displayed = new ArrayList<ErrorPresentacion>();
        var handler = new UiActionHandler(displayed::add);
        var success = new AtomicBoolean();
        var error = new ErrorPresentacion(CodigoError.PERSISTENCIA, "No se pudo acceder a los datos.");

        handler.run(() -> {
            failedController(error);
            success.set(true);
        });

        assertEquals(java.util.List.of(error), displayed);
        assertFalse(success.get());
    }

    @Test
    void permiteContinuarUsandoLaInterfazDespuesDeUnError() {
        var displayed = new ArrayList<ErrorPresentacion>();
        var handler = new UiActionHandler(displayed::add);
        handler.run(() -> failedController(new ErrorPresentacion(CodigoError.ESTADO, "Cupo agotado")));
        var success = new AtomicBoolean();

        handler.run(() -> success.set(true));

        assertTrue(success.get());
        assertEquals(1, displayed.size());
    }

    private void failedController(ErrorPresentacion error) {
        throw new PresentationException(error, new RuntimeException("Causa técnica"));
    }
}
