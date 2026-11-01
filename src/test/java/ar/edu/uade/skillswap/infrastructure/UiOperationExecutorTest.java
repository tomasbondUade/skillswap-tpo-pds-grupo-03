package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.model.Models.CodigoError;
import ar.edu.uade.skillswap.model.Models.ErrorPresentacion;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class UiOperationExecutorTest {
    private final UiOperationExecutor executor = new UiOperationExecutor();

    @Test
    void conservaElResultadoYLosEfectosDeOperacionesExitosas() {
        assertEquals("resultado", executor.execute(() -> "resultado"));
        var executed = new AtomicBoolean();
        executor.execute(() -> { executed.set(true); });
        assertTrue(executed.get());
    }

    @Test
    void traduceErroresDeAplicacionYConservaLaCausaParaDiagnostico() {
        var cause = new ApplicationException(ApplicationErrorCode.FORBIDDEN, "Acceso rechazado");
        var exception = failure(cause);
        assertEquals(CodigoError.PERMISOS, exception.error().codigo());
        assertEquals(exception.error().mensaje(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void traducePersistenciaSinMostrarSqlNiCredenciales() {
        String detail = "INSERT INTO wallet password=secreto";
        for (var cause : new RuntimeException[] {
                new PersistenceException(detail),
                new RollbackException(detail, new PersistenceException(detail)),
                new RuntimeException(detail, new SQLException(detail))}) {
            var exception = failure(cause);
            assertEquals(CodigoError.PERSISTENCIA, exception.error().codigo());
            assertFalse(exception.getMessage().contains("wallet"));
            assertFalse(exception.getMessage().contains("secreto"));
            assertSame(cause, exception.getCause());
        }
    }

    @Test
    void detectaOptimisticLockAunqueEsteDentroDelRollback() {
        for (var cause : new RuntimeException[] {
                new OptimisticLockException("version wallet"),
                new RollbackException("commit", new OptimisticLockException("version wallet"))}) {
            var exception = failure(cause);
            assertEquals(CodigoError.CONFLICTO, exception.error().codigo());
            assertFalse(exception.getMessage().contains("wallet"));
        }
    }

    @Test
    void noEnvuelveDosVecesUnErrorDePresentacion() {
        var original = new PresentationException(new ErrorPresentacion(CodigoError.VALIDACION,
                "Falta el objetivo"), new IllegalArgumentException());
        assertSame(original, failure(original));
    }

    @Test
    void traduceLosErroresDeOperacionesSinRetorno() {
        var exception = assertThrows(PresentationException.class, () -> executor.execute((Runnable) () -> {
            throw new IllegalArgumentException("Falta el identificador");
        }));
        assertEquals(CodigoError.VALIDACION, exception.error().codigo());
    }

    @Test
    void traduceErroresInesperadosSinConvertirlosEnValidaciones() {
        var exception = failure(new NullPointerException("Detalle interno"));
        assertEquals(CodigoError.INESPERADO, exception.error().codigo());
        assertFalse(exception.getMessage().contains("Detalle interno"));
    }

    private PresentationException failure(RuntimeException cause) {
        return assertThrows(PresentationException.class, () -> executor.execute(() -> {
            throw cause;
        }));
    }
}
