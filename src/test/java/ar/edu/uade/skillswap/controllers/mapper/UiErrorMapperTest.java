package ar.edu.uade.skillswap.controllers.mapper;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.model.Models.CodigoError;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UiErrorMapperTest {
    private final UiErrorMapper mapper = new UiErrorMapper();

    @Test
    void traduceTodasLasCategoriasDeAplicacionAUnContratoDePresentacion() {
        var expected = Map.of(ApplicationErrorCode.VALIDATION, CodigoError.VALIDACION,
                ApplicationErrorCode.UNAUTHENTICATED, CodigoError.NO_AUTENTICADO,
                ApplicationErrorCode.NOT_FOUND, CodigoError.NO_ENCONTRADO,
                ApplicationErrorCode.FORBIDDEN, CodigoError.PERMISOS,
                ApplicationErrorCode.INVALID_STATE, CodigoError.ESTADO,
                ApplicationErrorCode.CONFLICT, CodigoError.CONFLICTO);
        expected.forEach((applicationCode, uiCode) -> {
            var error = mapper.toError(new ApplicationException(applicationCode, "Mensaje del caso de uso"));
            assertEquals(uiCode, error.codigo());
            assertFalse(error.mensaje().isBlank());
        });
        assertEquals(ApplicationErrorCode.values().length, expected.size());
    }

    @Test
    void conservaLasExplicacionesDeValidacionYEstado() {
        assertEquals("El precio no puede ser negativo", mapper.toError(new IllegalArgumentException(
                "El precio no puede ser negativo")).mensaje());
        var error = mapper.toError(new ApplicationException(ApplicationErrorCode.INVALID_STATE,
                "Saldo insuficiente"));
        assertEquals(CodigoError.ESTADO, error.codigo());
        assertEquals("Saldo insuficiente", error.mensaje());
        assertEquals(CodigoError.ESTADO, mapper.toError(new IllegalStateException("Cupo agotado")).codigo());
    }

    @Test
    void usaMensajesPorDefectoCuandoNoHayExplicacion() {
        assertFalse(mapper.toError(new IllegalArgumentException()).mensaje().isBlank());
        assertFalse(mapper.toError(new IllegalStateException(" ")).mensaje().isBlank());
        assertFalse(mapper.toError(new ApplicationException(ApplicationErrorCode.NOT_FOUND, null))
                .mensaje().isBlank());
    }

    @Test
    void ocultaLosDetallesDeErroresInesperadosYPermisos() {
        String technicalDetail = "java.lang.NullPointerException en tabla wallet con clave secreta";
        var unexpected = mapper.toError(new NullPointerException(technicalDetail));
        assertEquals(CodigoError.INESPERADO, unexpected.codigo());
        assertFalse(unexpected.mensaje().contains(technicalDetail));
        var denied = mapper.toError(new SecurityException(technicalDetail));
        assertEquals(CodigoError.PERMISOS, denied.codigo());
        assertFalse(denied.mensaje().contains(technicalDetail));
    }
}
