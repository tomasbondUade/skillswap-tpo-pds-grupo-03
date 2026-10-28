package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityInput;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityView;
import ar.edu.uade.skillswap.application.model.PublicationData.*;
import ar.edu.uade.skillswap.application.port.in.PublicationUseCases;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

class PublicacionUiControllerTest {
    private final PublicationSpy service = new PublicationSpy();
    private final PublicacionUiController controller = new PublicacionUiController(service, () -> "u1", new UiOperationExecutor());
    private final LocalDateTime start = LocalDateTime.of(2026, 11, 4, 15, 0);

    @Test
    void convierteTodosLosDatosDelBorradorYElResultadoDelPuerto() {
        var result = controller.crearBorrador("u1", input());

        assertEquals(new CreateDraftCommand("Java", "Mentoría", "Tecnología", Modality.HYBRID,
                60, 3, 120, Set.of("Java"), List.of(new AvailabilityInput("slot", start,
                start.plusHours(1), 3))), service.create);
        assertEquals(1, service.calls);
        assertEquals("p1", result.id());
        assertEquals("Resultado del servicio", result.titulo());
        assertEquals(Models.EstadoPublicacion.BORRADOR, result.estado());
        assertEquals(Models.Modalidad.PRESENCIAL, result.modalidad());
        assertEquals(2, result.franjasDisponibles().get(0).cuposDisponibles());
    }

    @Test
    void conservaLaFirmaQueUsaLaPantallaYAgregaUnaFranjaDemo() {
        controller.crearBorrador("u1", "Java", "Mentoría", "Tecnología", Models.Modalidad.REMOTA, 45, 80);
        assertEquals(1, service.calls);
        assertEquals(6, service.create.capacity());
        assertEquals(80, service.create.priceInSkillCoins());
        assertEquals(1, service.create.availability().size());
        var slot = service.create.availability().get(0);
        assertEquals(45, java.time.Duration.between(slot.startsAt(), slot.endsAt()).toMinutes());
        assertEquals(6, slot.capacity());
    }

    @Test
    void delegaLaEdicionSinModificarPrecioNiDisponibilidadPorSuCuenta() {
        var result = controller.editarBorrador("u1", " p1 ", edition());
        assertEquals(new EditDraftCommand("p1", "Java avanzado", "Nueva descripción", "Tecnología",
                Modality.REMOTE, 90, 3, Set.of("OO")), service.edit);
        assertEquals(1, service.calls);
        assertEquals(120, result.precio());
        assertEquals("slot", result.franjasDisponibles().get(0).id());
    }

    @Test
    void delegaLaTransicionYElListadoDePublicacionesPropias() {
        controller.cambiarEstado("u1", " p1 ", Models.EstadoPublicacion.PAUSADA);
        assertEquals("p1", service.change.publicationId());
        assertEquals(PublicationStatus.PAUSED, service.change.targetStatus());
        assertFalse(service.change.reason().isBlank());
        var result = controller.listarPropias("u1");
        assertEquals(2, service.calls);
        assertEquals(List.of("p1"), result.stream().map(Models.Publicacion::id).toList());
    }

    @Test
    void rechazaSuplantacionEnTodasLasOperacionesAntesDeDelegar() {
        for (Supplier<?> action : actions(controller, "otra-persona")) {
            assertCode(Models.CodigoError.PERMISOS, action);
        }
        assertEquals(0, service.calls);
    }

    @Test
    void rechazaIdentidadAusenteEnTodasLasOperaciones() {
        var anonymous = new PublicacionUiController(service, () -> null, new UiOperationExecutor());
        for (Supplier<?> action : actions(anonymous, "u1")) {
            assertCode(Models.CodigoError.NO_AUTENTICADO, action);
        }
        assertEquals(0, service.calls);
    }

    @Test
    void validaEntradasFaltantesAntesDeDelegar() {
        assertCode(Models.CodigoError.VALIDACION, () -> controller.crearBorrador("u1", null));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.editarBorrador("u1", "p1", null));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.editarBorrador("u1", " ", edition()));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.cambiarEstado("u1", null, Models.EstadoPublicacion.PUBLICADA));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.cambiarEstado("u1", "p1", null));
        assertCode(Models.CodigoError.VALIDACION, () -> controller.listarPropias(" "));
        assertEquals(0, service.calls);
    }

    @Test
    void presentaLosErroresDeAutorEstadoYRecursoDevueltosPorAplicacion() {
        Map.of(ApplicationErrorCode.FORBIDDEN, Models.CodigoError.PERMISOS,
                ApplicationErrorCode.INVALID_STATE, Models.CodigoError.ESTADO,
                ApplicationErrorCode.NOT_FOUND, Models.CodigoError.NO_ENCONTRADO).forEach((application, ui) -> {
                    service.failure = new ApplicationException(application, "Rechazo del caso de uso");
                    assertCode(ui, () -> controller.editarBorrador("u1", "p1", edition()));
                });
    }

    private List<Supplier<?>> actions(PublicacionUiController target, String user) {
        return List.of(() -> target.listarPropias(user), () -> target.crearBorrador(user, input()),
                () -> target.crearBorrador(user, "Java", "Mentoría", "Tecnología", Models.Modalidad.REMOTA, 60, 80),
                () -> target.editarBorrador(user, "p1", edition()),
                () -> target.cambiarEstado(user, "p1", Models.EstadoPublicacion.PUBLICADA));
    }

    private Models.DatosBorrador input() {
        return new Models.DatosBorrador("Java", "Mentoría", "Tecnología", Models.Modalidad.HIBRIDA,
                60, 3, 120, List.of("Java"), List.of(new Models.Disponibilidad("slot", start, start.plusHours(1), 3)));
    }

    private Models.EdicionBorrador edition() {
        return new Models.EdicionBorrador("Java avanzado", "Nueva descripción", "Tecnología",
                Models.Modalidad.REMOTA, 90, 3, List.of("OO"));
    }

    private void assertCode(Models.CodigoError code, Supplier<?> action) {
        assertEquals(code, assertThrows(PresentationException.class, action::get).error().codigo());
    }

    private class PublicationSpy implements PublicationUseCases {
        private CreateDraftCommand create;
        private EditDraftCommand edit;
        private ChangeStatusCommand change;
        private int calls;
        private RuntimeException failure;

        @Override public PublicationView createDraft(CreateDraftCommand command) {
            create = command; return view();
        }
        @Override public PublicationView editDraft(EditDraftCommand command) {
            edit = command; return view();
        }
        @Override public PublicationView changeStatus(ChangeStatusCommand command) {
            change = command; return view();
        }
        @Override public List<PublicationView> listMyPublications() { return List.of(view()); }
        private PublicationView view() {
            calls++;
            if (failure != null) throw failure;
            return new PublicationView("p1", "u1", "Resultado del servicio", "Descripción", "Tecnología",
                    Modality.IN_PERSON, 60, 3, 120, Set.of("Java"), PublicationStatus.DRAFT,
                    List.of(new AvailabilityView("slot", start, start.plusHours(1), 3, 1)), start, 0);
        }
    }
}
