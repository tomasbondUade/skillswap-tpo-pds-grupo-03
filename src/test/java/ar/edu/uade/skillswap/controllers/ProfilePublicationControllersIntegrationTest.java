package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.service.ProfileApplicationService;
import ar.edu.uade.skillswap.application.service.PublicationApplicationService;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.infrastructure.InMemoryProfileRepository;
import ar.edu.uade.skillswap.infrastructure.InMemoryPublicationRepository;
import ar.edu.uade.skillswap.infrastructure.SynchronousTransactionManager;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Real use cases and domain, with memory repositories; no JPA or database connection. */
class ProfilePublicationControllersIntegrationTest {
    private String actor = "u1";
    private final InMemoryProfileRepository profileRepository = new InMemoryProfileRepository();
    private final InMemoryPublicationRepository publicationRepository = new InMemoryPublicationRepository();
    private final SynchronousTransactionManager transactions = new SynchronousTransactionManager();
    private final PerfilUiController profiles = new PerfilUiController(new ProfileApplicationService(
            profileRepository, () -> actor, transactions), () -> actor, new UiOperationExecutor());
    private final PublicacionUiController publications = new PublicacionUiController(new PublicationApplicationService(
            publicationRepository, () -> actor, transactions), () -> actor, new UiOperationExecutor());

    @Test
    void creaConsultaYRechazaUnPerfilDuplicado() {
        var created = profiles.crearPerfil("u1", profile("Julieta"));
        assertEquals("Julieta", created.nombre());
        assertEquals(created, profiles.verPerfil("u1"));
        assertCode(Models.CodigoError.CONFLICTO, () -> profiles.crearPerfil("u1", profile("Otro")));
        assertEquals("Julieta", profiles.verPerfil("u1").nombre());
    }

    @Test
    void editaPublicaYPausaDelegandoLasTransicionesAlDominio() {
        var draft = publications.crearBorrador("u1", draft());
        var edited = publications.editarBorrador("u1", draft.id(), edition("Java avanzado"));
        assertEquals("Java avanzado", edited.titulo());
        assertEquals(draft.precio(), edited.precio());
        assertEquals(draft.franjasDisponibles(), edited.franjasDisponibles());
        var published = publications.cambiarEstado("u1", draft.id(), Models.EstadoPublicacion.PUBLICADA);
        assertEquals(Models.EstadoPublicacion.PUBLICADA, published.estado());

        assertCode(Models.CodigoError.ESTADO,
                () -> publications.editarBorrador("u1", draft.id(), edition("No permitido")));

        var paused = publications.cambiarEstado("u1", draft.id(), Models.EstadoPublicacion.PAUSADA);
        assertEquals(Models.EstadoPublicacion.PAUSADA, paused.estado());
        assertEquals(List.of(paused), publications.listarPropias("u1"));
    }

    @Test
    void elUsuarioAutenticadoTampocoPuedeEditarNiPublicarRecursosAjenos() {
        profiles.crearPerfil("u1", profile("Julieta"));
        var draft = publications.crearBorrador("u1", draft());
        actor = "u2";
        assertTrue(publications.listarPropias("u2").isEmpty());
        assertCode(Models.CodigoError.PERMISOS,
                () -> publications.editarBorrador("u2", draft.id(), edition("Ajeno")));
        assertCode(Models.CodigoError.PERMISOS,
                () -> publications.cambiarEstado("u2", draft.id(), Models.EstadoPublicacion.PUBLICADA));
        assertCode(Models.CodigoError.PERMISOS, () -> profiles.verPerfil("u1"));
        actor = "u1";
        assertEquals(List.of(draft), publications.listarPropias("u1"));
    }

    @Test
    void losDatosInvalidosNoSeGuardanYElRecursoInexistenteEsNoEncontrado() {
        assertCode(Models.CodigoError.VALIDACION, () -> profiles.crearPerfil("u1", profile(" ")));
        assertTrue(profileRepository.findByUserId("u1").isEmpty());
        assertCode(Models.CodigoError.NO_ENCONTRADO, () -> profiles.verPerfil("u1"));
        assertCode(Models.CodigoError.NO_ENCONTRADO,
                () -> publications.editarBorrador("u1", "inexistente", edition("Java")));
        assertCode(Models.CodigoError.NO_ENCONTRADO,
                () -> publications.cambiarEstado("u1", "inexistente", Models.EstadoPublicacion.PUBLICADA));
        var start = LocalDateTime.now().plusDays(2);
        var invalid = new Models.DatosBorrador("Java", "Mentoría", "Tecnología", Models.Modalidad.REMOTA,
                60, 2, 100, List.of(), List.of(new Models.Disponibilidad(null, start, start.minusHours(1), 2)));
        assertCode(Models.CodigoError.VALIDACION, () -> publications.crearBorrador("u1", invalid));
        assertTrue(publications.listarPropias("u1").isEmpty());
    }

    private Models.NuevoPerfil profile(String name) {
        return new Models.NuevoPerfil(name, "Bio", "UTC", List.of("Java"), List.of("SQL"),
                Set.of(Models.CanalNotificacion.IN_APP), List.of());
    }

    private Models.DatosBorrador draft() {
        var start = LocalDateTime.now().plusDays(2);
        return new Models.DatosBorrador("Java", "Mentoría", "Tecnología", Models.Modalidad.REMOTA,
                60, 2, 100, List.of("OO"), List.of(new Models.Disponibilidad(null, start, start.plusHours(2), 2)));
    }

    private Models.EdicionBorrador edition(String title) {
        return new Models.EdicionBorrador(title, "Descripción editada", "Tecnología",
                Models.Modalidad.HIBRIDA, 90, 2, List.of("Java"));
    }

    private void assertCode(Models.CodigoError code, Supplier<?> action) {
        assertEquals(code, assertThrows(PresentationException.class, action::get).error().codigo());
    }
}
