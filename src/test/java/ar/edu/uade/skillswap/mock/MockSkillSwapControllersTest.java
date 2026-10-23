package ar.edu.uade.skillswap.mock;

import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockSkillSwapControllersTest {
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
}
