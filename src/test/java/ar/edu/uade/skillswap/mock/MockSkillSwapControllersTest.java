package ar.edu.uade.skillswap.mock;

import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockSkillSwapControllersTest {
    @Test
    void permiteBuscarPublicacionesPorTexto() {
        var controllers = new MockSkillSwapControllers();

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("Java", "Todas", null, 0, "Relevancia")).items();

        assertFalse(resultados.isEmpty());
        assertTrue(resultados.stream().anyMatch(p -> p.titulo().contains("Arquitectura")));
    }

    @Test
    void acreditarSkillCoinsActualizaElSaldo() {
        var controllers = new MockSkillSwapControllers();
        int saldoInicial = controllers.wallet().consultarSaldo("user-mateo");

        int nuevoSaldo = controllers.wallet().acreditarDemo("user-mateo", 500);

        assertEquals(saldoInicial + 500, nuevoSaldo);
    }

    @Test
    void solicitarMentoriaConservaObjetivoMensajeYFranjaSinReservarElCupo() {
        var controllers = new MockSkillSwapControllers();
        Publicacion publication = controllers.busqueda().buscar(
                        new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items()
                .get(0);
        var selectedSlot = publication.franjasDisponibles().get(0);

        var booking = controllers.contrataciones().solicitar("user-mateo", publication.id(),
                "  Estoy preparando una entrevista  ", "  Practicar algoritmos  ", selectedSlot.id());

        assertEquals("Estoy preparando una entrevista", booking.mensajeCliente());
        assertEquals("Practicar algoritmos", booking.objetivo());
        assertEquals(selectedSlot.id(), booking.franjaId());
        assertEquals(selectedSlot.inicio(), booking.fecha());
        assertEquals(EstadoContratacion.PENDIENTE, booking.estado());

        Publicacion currentPublication = controllers.busqueda().buscar(
                        new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items()
                .stream().filter(candidate -> candidate.id().equals(publication.id())).findFirst().orElseThrow();
        assertEquals(1, currentPublication.franjasDisponibles().get(0).cuposDisponibles(),
                "Una solicitud pendiente no reserva el cupo; se mantiene la regla del dominio");

        var advancedBooking = controllers.contrataciones().avanzar("user-mateo", booking.id());
        assertEquals(EstadoContratacion.ACEPTADA, advancedBooking.estado());
        assertEquals("Practicar algoritmos", advancedBooking.objetivo());
        assertEquals("Estoy preparando una entrevista", advancedBooking.mensajeCliente());
    }

    @Test
    void solicitarMentoriaRechazaObjetivoVacioYFranjaAjena() {
        var controllers = new MockSkillSwapControllers();
        Publicacion publication = controllers.busqueda().buscar(
                        new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).items()
                .get(0);
        String validSlot = publication.franjasDisponibles().get(0).id();

        assertThrows(IllegalArgumentException.class, () -> controllers.contrataciones().solicitar(
                "user-mateo", publication.id(), "", " ", validSlot));
        assertThrows(IllegalArgumentException.class, () -> controllers.contrataciones().solicitar(
                "user-mateo", publication.id(), "", "Objetivo válido", "slot-ajeno"));
        assertThrows(IllegalArgumentException.class, () -> controllers.contrataciones().solicitar(
                "user-mateo", "missing-publication", "", "Objetivo válido", validSlot));

        assertEquals(2, controllers.contrataciones().listarContrataciones("user-mateo").size());
    }

    @Test
    void elMockPaginaYAplicaLosFiltrosNuevos() {
        var controllers = new MockSkillSwapControllers();

        var page = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, null, null,
                null, "Menor precio", 1, 4));
        var bySkill = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, "java", null, null,
                null, null, 0, 20));
        var byPriceAndReputation = controllers.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, null, 200,
                null, 4.9, null, 0, 20));

        assertEquals(6, page.total());
        assertEquals(2, page.totalPaginas());
        assertEquals(2, page.items().size());
        assertEquals(Set.of("p1", "p3"), bySkill.items().stream().map(Publicacion::id)
                .collect(Collectors.toSet()));
        assertEquals(Set.of("p3", "p5"), byPriceAndReputation.items().stream().map(Publicacion::id)
                .collect(Collectors.toSet()));
        assertEquals(3, controllers.busqueda().recomendar("user-mateo").size());
    }
}
