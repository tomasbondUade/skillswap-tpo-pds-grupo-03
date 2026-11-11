package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import java.util.List;
import java.util.Set;
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
                .crearBorrador("user-mateo", "Mentoría", "Descripción", "Tecnología", null, 60, 100,
                        Models.TipoPublicacion.MENTORIA));
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
                new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items().stream()
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
                "Descripción", "Tecnología", Modalidad.REMOTA, 60, 100, Models.TipoPublicacion.MENTORIA);

        var exception = assertThrows(PresentationException.class,
                () -> controllers.publicaciones().cambiarEstado("user-mateo", draft.id(),
                        Models.EstadoPublicacion.PAUSADA));

        assertEquals(Models.CodigoError.ESTADO, exception.error().codigo());
    }

    @Test
    void usaLaWiringDeAplicacionParaBusquedaYPerfil() {
        var controllers = new WiredUiControllers();

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("Java", "Todas", Modalidad.REMOTA, 0, "Relevancia")).items();

        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream().anyMatch(p -> p.titulo().toLowerCase().contains("java")));

        var perfil = controllers.perfiles().verPerfil("user-mateo");
        assertFalse(perfil.habilidades().isEmpty());
    }

    @Test
    void laBusquedaMuestraElNombreYLaReputacionRealesDelAutor() {
        var controllers = new WiredUiControllers();

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("entrevistas", "Todas", null, 0, "Relevancia")).items();

        assertEquals(1, resultados.size());
        assertEquals("Ana García", resultados.get(0).proveedor());
        assertEquals(0.0, resultados.get(0).reputacion());
    }

    @Test
    void laBusquedaDevuelvePaginasConElTotalYNavegacion() {
        var controllers = new WiredUiControllers();

        var first = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                null, "Menor precio", 0, 2));
        var second = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                null, "Menor precio", 1, 2));

        assertEquals(3, first.total());
        assertEquals(2, first.totalPaginas());
        assertEquals(List.of(150, 180), first.items().stream().map(Models.Publicacion::precio).toList());
        assertFalse(first.hayAnterior());
        assertTrue(first.haySiguiente());
        assertEquals(List.of(220), second.items().stream().map(Models.Publicacion::precio).toList());
        assertTrue(second.hayAnterior());
        assertFalse(second.haySiguiente());
    }

    @Test
    void laBusquedaAplicaLosFiltrosNuevos() {
        var controllers = new WiredUiControllers();

        var bySkill = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, "backend", null, null,
                null, null, 0, 20));
        var byModality = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", Modalidad.PRESENCIAL,
                null, null, null, null, null, 0, 20));
        var byPrice = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, 160, 200,
                null, null, 0, 20));
        var byReputation = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                1.0, null, 0, 20));

        assertEquals(List.of("Ana García"), bySkill.items().stream().map(Models.Publicacion::proveedor).toList());
        assertEquals(List.of("Lucía Pérez"), byModality.items().stream().map(Models.Publicacion::proveedor).toList());
        assertEquals(List.of(180), byPrice.items().stream().map(Models.Publicacion::precio).toList());
        assertEquals(0, byReputation.total());
    }

    @Test
    void losFiltrosInvalidosSeInformanComoValidacion() {
        var controllers = new WiredUiControllers();

        var error = assertThrows(PresentationException.class, () -> controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, null, 300, 100, null, null, 0, 20)));

        assertEquals(Models.CodigoError.VALIDACION, error.error().codigo());
    }

    @Test
    void noRecomiendaParaOtroUsuario() {
        var controllers = new WiredUiControllers();

        var error = assertThrows(PresentationException.class,
                () -> controllers.busqueda().recomendar("mentor-ana"));

        assertEquals(Models.CodigoError.PERMISOS, error.error().codigo());
    }

    @Test
    void lasRecomendacionesMuestranElNombreYLaReputacionRealesDelAutor() {
        var controllers = new WiredUiControllers();

        var recomendaciones = controllers.busqueda().recomendar("user-mateo");

        assertFalse(recomendaciones.isEmpty());
        assertTrue(recomendaciones.stream().allMatch(p ->
                Set.of("Mateo Silva", "Ana García", "Lucía Pérez").contains(p.proveedor())));
        assertTrue(recomendaciones.stream().allMatch(p -> p.reputacion() == 0.0));
    }

        @Test
        void conectaContratacionesWalletYNotificacionesConAplicacion() {
        var controllers = new WiredUiControllers();
        var publication = controllers.busqueda().buscar(
                new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items().stream()
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
    void ofreceLosTiposDelUsuarioYCreaElBorradorDelTipoElegido() {
        var controllers = new WiredUiControllers();

        assertEquals(List.of(Models.TipoPublicacion.MENTORIA, Models.TipoPublicacion.SERVICIO),
                controllers.publicaciones().tiposPublicacion("user-mateo"));
        var service = controllers.publicaciones().crearBorrador("user-mateo", "Revisión de CV",
                "Correcciones y sugerencias.", "Tecnología", Modalidad.REMOTA, 60, 90,
                Models.TipoPublicacion.SERVICIO);
        var withoutType = assertThrows(PresentationException.class, () -> controllers.publicaciones()
                .crearBorrador("user-mateo", "Mentoría Java", "Sesión práctica.", "Tecnología",
                        Modalidad.REMOTA, 60, 90, null));

        assertEquals(Models.TipoPublicacion.SERVICIO, service.tipo());
        assertEquals(Models.CodigoError.VALIDACION, withoutType.error().codigo());
        assertTrue(controllers.publicaciones().listarPropias("user-mateo").stream()
                .anyMatch(item -> item.id().equals(service.id())
                        && item.tipo() == Models.TipoPublicacion.SERVICIO));
    }

    @Test
    void rechazaUnTipoQueElUsuarioNoPuedeCrearComoPermisos() {
        var controllers = new WiredUiControllers();
        int before = controllers.publicaciones().listarPropias("user-mateo").size();

        var error = assertThrows(PresentationException.class, () -> controllers.publicaciones().crearBorrador(
                "user-mateo", "Hackatón", "Desafío abierto.", "Tecnología", Modalidad.REMOTA, 60, 90,
                Models.TipoPublicacion.DESAFIO));

        assertEquals(Models.CodigoError.PERMISOS, error.error().codigo());
        assertEquals(before, controllers.publicaciones().listarPropias("user-mateo").size());
    }

        @Test
        void creaBorradoresPublicablesConDisponibilidadValida() {
        var controllers = new WiredUiControllers();
        var draft = controllers.publicaciones().crearBorrador("user-mateo", "Mentoría de integración",
            "Práctica de arquitectura y testing.", "Tecnología", Modalidad.REMOTA, 60, 120,
            Models.TipoPublicacion.MENTORIA);

        var published = controllers.publicaciones().cambiarEstado("user-mateo", draft.id(),
            Models.EstadoPublicacion.PUBLICADA);

        assertEquals(Models.EstadoPublicacion.PUBLICADA, published.estado());
        assertTrue(controllers.busqueda().buscar(
                new FiltrosBusqueda("Mentoría de integración", "Todas", null, 0, "Relevancia")).items()
            .stream().anyMatch(item -> item.id().equals(draft.id())));
        }
}
