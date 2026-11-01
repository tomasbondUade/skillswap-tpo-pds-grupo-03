package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WiredUiControllersTest {
    @Test
    void presentaLosErroresDeEntradaConUnContratoUniforme() {
        var controllers = new WiredUiControllers();
        var search = assertThrows(PresentationException.class, () -> controllers.busqueda().buscar(null));
        assertEquals(Models.CodigoError.VALIDACION, search.error().codigo());
        var publication = assertThrows(PresentationException.class, () -> controllers.publicaciones()
                .crearBorrador("user-mateo", "Mentoría", "Descripción", "Tecnología", null, 60, 100));
        assertEquals(Models.CodigoError.VALIDACION, publication.error().codigo());
        var wallet = assertThrows(PresentationException.class,
                () -> controllers.wallet().acreditarDemo("user-mateo", -10));
        assertEquals(Models.CodigoError.VALIDACION, wallet.error().codigo());
    }

    @Test
    void presentaNoEncontradoEnOperacionesConYSinRetorno() {
        var controllers = new WiredUiControllers();
        var booking = assertThrows(PresentationException.class,
                () -> controllers.contrataciones().avanzar("user-mateo", "inexistente"));
        assertEquals(Models.CodigoError.NO_ENCONTRADO, booking.error().codigo());
        var notification = assertThrows(PresentationException.class,
                () -> controllers.notificaciones().marcarLeida("user-mateo", "inexistente"));
        assertEquals(Models.CodigoError.NO_ENCONTRADO, notification.error().codigo());
    }

    @Test
    void presentaErroresDePermisosSinCambiarLaContratacion() {
        var controllers = new WiredUiControllers();
        var publication = controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).stream()
                .filter(item -> !"Mateo Silva".equals(item.proveedor())).findFirst().orElseThrow();
        var booking = controllers.contrataciones().solicitar("user-mateo", publication.id(),
                "Hola", "Aprender", publication.franjasDisponibles().get(0).id());

        var exception = assertThrows(PresentationException.class,
                () -> controllers.contrataciones().avanzar("user-mateo", booking.id()));

        assertEquals(Models.CodigoError.PERMISOS, exception.error().codigo());
        assertEquals(EstadoContratacion.PENDIENTE, controllers.contrataciones()
                .listarContrataciones("user-mateo").get(0).estado());
    }

    @Test
    void presentaLasTransicionesInvalidasComoErroresDeEstado() {
        var controllers = new WiredUiControllers();
        var draft = controllers.publicaciones().crearBorrador("user-mateo", "Mentoría",
                "Descripción", "Tecnología", Modalidad.REMOTA, 60, 100);

        var exception = assertThrows(PresentationException.class,
                () -> controllers.publicaciones().cambiarEstado("user-mateo", draft.id(),
                        Models.EstadoPublicacion.PAUSADA));

        assertEquals(Models.CodigoError.ESTADO, exception.error().codigo());
    }

    @Test
    void usaLaWiringDeAplicacionParaBusquedaYPerfil() {
        var controllers = new WiredUiControllers();

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("Java", "Todas", Modalidad.REMOTA, 0, "Relevancia"));

        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream().anyMatch(p -> p.titulo().toLowerCase().contains("java")));

        var perfil = controllers.perfiles().verPerfil("user-mateo");
        assertFalse(perfil.habilidades().isEmpty());
    }

        @Test
        void conectaContratacionesWalletYNotificacionesConAplicacion() {
        var controllers = new WiredUiControllers();
        var publication = controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).stream()
            .filter(item -> !"Mateo Silva".equals(item.proveedor()))
            .findFirst()
            .orElseThrow();

        var requested = controllers.contrataciones().solicitar(
            "user-mateo", publication.id(), "Hola, ¿tenés material previo?", "Practicar el tema",
            publication.franjasDisponibles().get(0).id());
        assertEquals(EstadoContratacion.PENDIENTE, requested.estado());
        assertFalse(controllers.contrataciones().puedeAvanzar("user-mateo", requested.id()));
        var canceled = controllers.contrataciones().cancelar("user-mateo", requested.id());
        assertEquals(EstadoContratacion.CANCELADA, canceled.estado());

        int initialBalance = controllers.wallet().consultarSaldo("user-mateo");
        assertEquals(initialBalance + 300, controllers.wallet().acreditarDemo("user-mateo", 300));
        assertTrue(controllers.wallet().listarMovimientos("user-mateo").stream()
            .anyMatch(movement -> movement.concepto().equals("Créditos demo")));

        assertFalse(controllers.notificaciones().listarNotificaciones("user-mateo").isEmpty());
        controllers.notificaciones().marcarTodasLeidas("user-mateo");
        assertTrue(controllers.notificaciones().listarNotificaciones("user-mateo").stream()
            .allMatch(Models.Notificacion::leida));
        }

        @Test
        void creaBorradoresPublicablesConDisponibilidadValida() {
        var controllers = new WiredUiControllers();
        var draft = controllers.publicaciones().crearBorrador("user-mateo", "Mentoría de integración",
            "Práctica de arquitectura y testing.", "Tecnología", Modalidad.REMOTA, 60, 120);

        var published = controllers.publicaciones().cambiarEstado("user-mateo", draft.id(),
            Models.EstadoPublicacion.PUBLICADA);

        assertEquals(Models.EstadoPublicacion.PUBLICADA, published.estado());
        assertTrue(controllers.busqueda().buscar(
                new FiltrosBusqueda("Mentoría de integración", "Todas", null, 0, "Relevancia"))
            .stream().anyMatch(item -> item.id().equals(draft.id())));
        }
}
