package ar.edu.uade.skillswap.mock;

import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockSkillSwapControllersTest {
    @Test
    void mantieneLaCompatibilidadDelMockConLosNuevosContratos() {
        var controllers = new MockSkillSwapControllers();
        var profile = controllers.perfiles().crearPerfil("nuevo-usuario",
                new ar.edu.uade.skillswap.model.Models.NuevoPerfil("Julieta", "Bio", "UTC",
                        java.util.List.of("Java"), java.util.List.of(), java.util.Set.of(), java.util.List.of()));
        assertEquals(profile, controllers.perfiles().verPerfil("nuevo-usuario"));
        var draft = controllers.publicaciones().crearBorrador("nuevo-usuario",
                new ar.edu.uade.skillswap.model.Models.DatosBorrador("Java", "Mentoría", "Tecnología",
                        ar.edu.uade.skillswap.model.Models.Modalidad.REMOTA, 60, 2, 100,
                        java.util.List.of("Java"), java.util.List.of()));
        var edited = controllers.publicaciones().editarBorrador("nuevo-usuario", draft.id(),
                new ar.edu.uade.skillswap.model.Models.EdicionBorrador("Java avanzado", "Bio", "Tecnología",
                        ar.edu.uade.skillswap.model.Models.Modalidad.HIBRIDA, 60, 2, java.util.List.of("OO")));
        assertEquals("Java avanzado", edited.titulo());
        assertEquals(java.util.List.of(edited), controllers.publicaciones().listarPropias("nuevo-usuario"));
    }

    @Test
    void permiteBuscarPublicacionesPorTexto() {
        var controllers = new MockSkillSwapControllers();

        var resultados = controllers.busqueda().buscar(
                new FiltrosBusqueda("Java", "Todas", null, 0, "Relevancia"));

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
                        new FiltrosBusqueda("", "Todas", null, 0, "Relevancia"))
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
                        new FiltrosBusqueda("", "Todas", null, 0, "Relevancia"))
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
                        new FiltrosBusqueda("", "Todas", null, 0, "Relevancia"))
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
}
