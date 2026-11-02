package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.error.*;
import ar.edu.uade.skillswap.application.model.BookingData.*;
import ar.edu.uade.skillswap.application.port.in.BookingUseCases;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ContratacionUiControllerTest {
    private final Spy service = new Spy();
    private ContratacionUiController controller(String actor) {
        return new ContratacionUiController(service, () -> actor, new UiOperationExecutor(),
                view -> new Models.Contratacion(view.id(), "Mentoría", "Mentor", view.createdAt(),
                        view.priceInSkillCoins(), new ar.edu.uade.skillswap.controllers.mapper.UiModelMapper()
                        .toEstadoContratacion(view.status()), view.objective(), view.clientMessage(), view.slotId()));
    }

    @Test void delegaLasOperacionesYConvierteLaSalida() {
        var controller = controller("u1");
        assertEquals("Mentoría", controller.solicitar("u1", " pub ", "Hola", " Aprender ", " slot ").titulo());
        assertEquals(new RequestCommand("pub", "Hola", "Aprender", "slot"), service.command);
        controller.aceptar("u1", " b "); assertEquals("accept", service.operation);
        controller.rechazar("u1", "b", " Motivo "); assertEquals(new RejectCommand("b", "Motivo"), service.command);
        controller.pagar("u1", "b"); assertEquals("pay", service.operation);
        controller.cancelar("u1", "b", " Motivo "); assertEquals(new CancelCommand("b", "Motivo"), service.command);
        controller.proponerReprogramacion("u1", "b", " nueva ");
        assertEquals(new RescheduleCommand("b", "nueva"), service.command);
        controller.aceptarReprogramacion("u1", "b"); assertEquals("acceptReschedule", service.operation);
        controller.iniciar("u1", "b"); assertEquals("start", service.operation);
        controller.completar("u1", "b"); assertEquals("complete", service.operation);
        assertEquals(9, service.calls);
        assertEquals(1, controller.listarContrataciones("u1").size());
    }

    @Test void rechazaSuplantacionYFaltaDeIdentidadAntesDeDelegar() {
        var controller = controller("u1");
        assertCode(Models.CodigoError.PERMISOS, () -> controller.solicitar("u2", "p", "", "o", "s"));
        assertCode(Models.CodigoError.PERMISOS, () -> controller.pagar("u2", "b"));
        assertCode(Models.CodigoError.PERMISOS, () -> controller.cancelar("u2", "b"));
        assertCode(Models.CodigoError.PERMISOS, () -> controller.listarContrataciones("u2"));
        assertCode(Models.CodigoError.NO_AUTENTICADO, () -> controller(null).aceptar("u1", "b"));
        assertEquals(0, service.calls);
    }

    @Test void validaLosDatosObligatorios() {
        var controller = controller("u1");
        assertCode(Models.CodigoError.VALIDACION, () -> controller.solicitar("u1", " ", "", "o", "s"));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.solicitar("u1", "p", "", " ", "s"));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.solicitar("u1", "p", "", "o", " "));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.rechazar("u1", "b", " "));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.proponerReprogramacion("u1", "b", null));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.completar("u1", null));
        assertEquals(0, service.calls);
    }

    @Test void traduceLosErroresDeAplicacion() {
        var controller = controller("u1");
        Map.of(ApplicationErrorCode.FORBIDDEN, Models.CodigoError.PERMISOS,
                ApplicationErrorCode.INVALID_STATE, Models.CodigoError.ESTADO,
                ApplicationErrorCode.NOT_FOUND, Models.CodigoError.NO_ENCONTRADO,
                ApplicationErrorCode.CONFLICT, Models.CodigoError.CONFLICTO,
                ApplicationErrorCode.VALIDATION, Models.CodigoError.VALIDACION)
                .forEach((code, expected) -> {
                    service.failure = new ApplicationException(code, "Operación rechazada");
                    assertCode(expected, () -> controller.pagar("u1", "b"));
                });
    }

    @Test void avanzarRespetaEstadosYParticipantes() {
        var controller = controller("u1");
        service.status = BookingStatus.PENDING;
        assertFalse(controller.puedeAvanzar("u1", "b"));
        assertTrue(controller("u2").puedeAvanzar("u2", "b"));
        controller.avanzar("u1", "b"); assertEquals("accept", service.operation);
        service.status = BookingStatus.ACCEPTED;
        controller.avanzar("u1", "b"); assertEquals("pay", service.operation);
        service.status = BookingStatus.CONFIRMED;
        controller.avanzar("u1", "b"); assertEquals("start", service.operation);
        service.status = BookingStatus.IN_PROGRESS;
        controller.avanzar("u1", "b"); assertEquals("complete", service.operation);
        service.status = BookingStatus.COMPLETED;
        assertFalse(controller.puedeAvanzar("u1", "b"));
        assertCode(Models.CodigoError.ESTADO, () -> controller.avanzar("u1", "b"));
        service.visible = false;
        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> controller.puedeAvanzar("u1", "b"));
    }

    private static void assertCode(Models.CodigoError expected, Runnable action) {
        assertEquals(expected, assertThrows(PresentationException.class, action::run).error().codigo());
    }

    private static class Spy implements BookingUseCases {
        private Object command;
        private String operation;
        private int calls;
        private BookingStatus status = BookingStatus.PENDING;
        private boolean visible = true;
        private RuntimeException failure;
        private BookingView view() {
            return new BookingView("b", "pub", "u1", "u2", "Aprender", "Hola", "slot",
                    100, 0, status, LocalDateTime.of(2026, 10, 28, 12, 0), null);
        }
        private BookingView call(String name, Object input) {
            operation = name; command = input; calls++;
            if (failure != null) throw failure;
            return view();
        }
        @Override public List<BookingView> listMyBookings() {
            calls++;
            return visible ? List.of(view()) : List.of();
        }
        @Override public BookingView request(RequestCommand c) { return call("request", c); }
        @Override public BookingView accept(BookingCommand c) { return call("accept", c); }
        @Override public BookingView reject(RejectCommand c) { return call("reject", c); }
        @Override public BookingView pay(BookingCommand c) { return call("pay", c); }
        @Override public BookingView cancel(CancelCommand c) { return call("cancel", c); }
        @Override public BookingView proposeReschedule(RescheduleCommand c) { return call("proposeReschedule", c); }
        @Override public BookingView acceptReschedule(BookingCommand c) { return call("acceptReschedule", c); }
        @Override public BookingView start(BookingCommand c) { return call("start", c); }
        @Override public BookingView complete(BookingCommand c) { return call("complete", c); }
    }
}
