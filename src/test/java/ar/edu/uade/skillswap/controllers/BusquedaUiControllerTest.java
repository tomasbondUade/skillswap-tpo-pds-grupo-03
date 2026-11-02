package ar.edu.uade.skillswap.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.controllers.error.PresentationException;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.infrastructure.UiOperationExecutor;
import ar.edu.uade.skillswap.model.Models;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusquedaUiControllerTest {
    private final DiscoverySpy discovery = new DiscoverySpy();
    private final BusquedaUiController controller =
            new BusquedaUiController(discovery, () -> "user-1", new UiOperationExecutor());

    @Test
    void convierteTodosLosFiltrosElOrdenYLaPaginaALaEntradaDeAplicacion() {
        controller.buscar(new FiltrosBusqueda(" java ", "Tecnología", Models.Modalidad.HIBRIDA, " Backend ",
                100, 200, 4.0, "Más recientes", 2, 5));

        assertEquals(new SearchQuery("java", "Tecnología", Modality.HYBRID, "Backend", 100, 200, 4.0,
                SearchOrder.MOST_RECENT, 2, 5), discovery.query);
    }

    @Test
    void sinFiltrosUsaTodasLasCategoriasYOrdenPorRelevancia() {
        controller.buscar(new FiltrosBusqueda("", "Todas", null, null, null, null, null, null, 0, 20));

        assertNull(discovery.query.category());
        assertNull(discovery.query.modality());
        assertEquals(SearchOrder.RELEVANCE, discovery.query.order());
    }

    @Test
    void mapeaCadaEtiquetaDeOrden() {
        var expected = List.of(SearchOrder.RELEVANCE, SearchOrder.BEST_RATING, SearchOrder.LOWEST_PRICE,
                SearchOrder.MOST_RECENT);
        var labels = List.of("Relevancia", "Mejor reputación", "Menor precio", "Más recientes");
        for (int index = 0; index < labels.size(); index++) {
            controller.buscar(new FiltrosBusqueda("", "Todas", null, 0, labels.get(index)));
            assertEquals(expected.get(index), discovery.query.order(), labels.get(index));
        }
    }

    @Test
    void devuelveUnaPaginaConModelosDeUiYDatosDelAutor() {
        discovery.page = new PageResult<>(List.of(DiscoverySpy.view("p2", "Ana García", 4.5)), 1, 1, 3);

        Models.PaginaPublicaciones page = controller.buscar(new FiltrosBusqueda("", "Todas", null, 0, null));

        assertEquals(1, page.pagina());
        assertEquals(1, page.tamanio());
        assertEquals(3, page.total());
        assertEquals(3, page.totalPaginas());
        assertTrue(page.hayAnterior());
        assertTrue(page.haySiguiente());
        Models.Publicacion item = page.items().get(0);
        assertEquals("p2", item.id());
        assertEquals("Ana García", item.proveedor());
        assertEquals(4.5, item.reputacion());
        assertEquals(Models.Modalidad.HIBRIDA, item.modalidad());
        assertEquals(3, item.franjasDisponibles().get(0).cuposDisponibles());
    }

    @Test
    void unaPaginaVaciaNoTieneAnteriorNiSiguiente() {
        Models.PaginaPublicaciones page = controller.buscar(new FiltrosBusqueda("", "Todas", null, 0, null));

        assertEquals(0, page.total());
        assertFalse(page.hayAnterior());
        assertFalse(page.haySiguiente());
    }

    @Test
    void losErroresDeFiltrosYPaginacionSeInformanComoValidacionSinLlamarAlCasoDeUso() {
        List<FiltrosBusqueda> invalid = List.of(
                new FiltrosBusqueda("", "Todas", null, null, -1, null, null, null, 0, 20),
                new FiltrosBusqueda("", "Todas", null, null, 300, 100, null, null, 0, 20),
                new FiltrosBusqueda("", "Todas", null, null, null, null, 6.0, null, 0, 20),
                new FiltrosBusqueda("", "Todas", null, null, null, null, null, null, -1, 20),
                new FiltrosBusqueda("", "Todas", null, null, null, null, null, null, 0, 101),
                new FiltrosBusqueda("", "Todas", null, null, null, null, null, "Alfabético", 0, 20));
        for (FiltrosBusqueda filtros : invalid) {
            var error = assertThrows(PresentationException.class, () -> controller.buscar(filtros));
            assertEquals(Models.CodigoError.VALIDACION, error.error().codigo(), filtros.toString());
        }
        var missing = assertThrows(PresentationException.class, () -> controller.buscar(null));
        assertEquals(Models.CodigoError.VALIDACION, missing.error().codigo());
        assertEquals(0, discovery.calls);
    }

    @Test
    void lasCategoriasEmpiezanConTodas() {
        assertEquals(List.of("Todas", "Diseño", "Tecnología"), controller.categorias());
    }

    @Test
    void recomiendaConLaCantidadFijaYMapeaElAutor() {
        discovery.recommendations = List.of(DiscoverySpy.view("p1", "Lucía Pérez", 4.8));

        List<Models.Publicacion> result = controller.recomendar("user-1");

        assertEquals(BusquedaUiController.CANTIDAD_RECOMENDACIONES, discovery.limit);
        assertEquals(1, result.size());
        assertEquals("Lucía Pérez", result.get(0).proveedor());
        assertEquals(4.8, result.get(0).reputacion());
    }

    @Test
    void soloRecomiendaParaElUsuarioActivo() {
        var other = assertThrows(PresentationException.class, () -> controller.recomendar("user-2"));
        assertEquals(Models.CodigoError.PERMISOS, other.error().codigo());

        var blank = assertThrows(PresentationException.class, () -> controller.recomendar(" "));
        assertEquals(Models.CodigoError.VALIDACION, blank.error().codigo());

        var anonymous = new BusquedaUiController(discovery, () -> null, new UiOperationExecutor());
        var unauthenticated = assertThrows(PresentationException.class, () -> anonymous.recomendar("user-1"));
        assertEquals(Models.CodigoError.NO_AUTENTICADO, unauthenticated.error().codigo());

        assertEquals(0, discovery.calls);
    }
}
