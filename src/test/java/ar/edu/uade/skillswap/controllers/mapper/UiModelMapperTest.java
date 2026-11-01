package ar.edu.uade.skillswap.controllers.mapper;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityView;
import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.model.WalletData.MovementView;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.model.Models;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UiModelMapperTest {
    private final UiModelMapper mapper = new UiModelMapper();
    private final LocalDateTime fecha = LocalDateTime.of(2026, 11, 3, 15, 0);

    @Test
    void conviertePublicacionYCalculaCuposDisponibles() {
        var view = new PublicationView("p1", "u1", "Java", "Mentoría", "Tecnología",
                Modality.REMOTE, 60, 4, 120, Set.of("Java"), PublicationStatus.PUBLISHED,
                List.of(new AvailabilityView("f1", fecha, fecha.plusHours(1), 4, 3)), fecha, 2);

        var dto = mapper.toPublicacion(view, "Julieta", 4.5);

        assertEquals("p1", dto.id());
        assertEquals("Java", dto.titulo());
        assertEquals("Mentoría", dto.descripcion());
        assertEquals("Tecnología", dto.categoria());
        assertEquals(Models.Modalidad.REMOTA, dto.modalidad());
        assertEquals(60, dto.duracionMinutos());
        assertEquals(120, dto.precio());
        assertEquals("Julieta", dto.proveedor());
        assertEquals(4.5, dto.reputacion());
        assertEquals(List.of("Java"), dto.etiquetas());
        assertEquals(Models.EstadoPublicacion.PUBLICADA, dto.estado());
        assertFalse(dto.destacada());
        assertEquals(new Models.FranjaDisponibilidad("f1", fecha, fecha.plusHours(1), 1),
                dto.franjasDisponibles().get(0));
    }

    @Test
    void conservaLosDatosDeLaSolicitud() {
        var view = new BookingView("b1", "p1", "comprador", "proveedor", "Aprender Java",
                "Hola", "f1", 120, 0, BookingStatus.PENDING, fecha, null);

        assertEquals(new Models.Contratacion("b1", "Java", "Julieta", fecha, 120,
                Models.EstadoContratacion.PENDIENTE, "Aprender Java", "Hola", "f1"),
                mapper.toContratacion(view, "Java", "Julieta"));
    }

    @Test
    void conviertePerfilMovimientosYNotificaciones() {
        var view = new ProfileView("perfil", "u1", "Julieta", "Mentora", "UTC",
                List.of("Java"), List.of(), Set.of(), List.of(), 4.5, 3, true);

        assertEquals(new Models.Perfil("u1", "Julieta", "Mentora", "Mentora",
                List.of("Java"), 4.5, 3, 2), mapper.toPerfil(view, "Mentora", 2));
        assertEquals(new Models.Movimiento("Pago", fecha, -120, 380),
                mapper.toMovimiento(new MovementView("m1", "Pago", -120, 380, fecha)));
        assertEquals(new Models.Notificacion("n1", "Pago", "Confirmado", fecha, false, "PAGO"),
                mapper.toNotificacion(new NotificationView("n1", "Pago", "Confirmado", fecha, false, "PAGO")));
    }

    @Test
    void convierteFiltrosYRespetaLosValoresPorDefectoDeLaInterfaz() {
        var defaults = mapper.toSearchQuery(new Models.FiltrosBusqueda(" Java ", "Todas", null, 0, null));
        assertEquals("Java", defaults.text());
        assertNull(defaults.category());
        assertNull(defaults.modality());
        assertNull(defaults.maximumPrice());
        assertEquals(SearchOrder.RELEVANCE, defaults.order());
        assertEquals(0, defaults.page());
        assertEquals(20, defaults.size());

        var query = mapper.toSearchQuery(new Models.FiltrosBusqueda("Java", "Tecnología",
                Models.Modalidad.HIBRIDA, 250, "Menor precio"));
        assertEquals("Tecnología", query.category());
        assertEquals(Modality.HYBRID, query.modality());
        assertEquals(250, query.maximumPrice());
        assertEquals(SearchOrder.LOWEST_PRICE, query.order());
        assertEquals(SearchOrder.BEST_RATING, mapper.toSearchQuery(
                new Models.FiltrosBusqueda("", null, null, 0, "Mejor reputación")).order());
    }

    @Test
    void convierteTodasLasModalidadesYEstadosSinDependerDelOrdenDeLosEnums() {
        var modalities = java.util.Map.of(Modality.REMOTE, Models.Modalidad.REMOTA,
                Modality.IN_PERSON, Models.Modalidad.PRESENCIAL, Modality.HYBRID, Models.Modalidad.HIBRIDA);
        modalities.forEach((domain, ui) -> {
            assertEquals(ui, mapper.toModalidad(domain));
            assertEquals(domain, mapper.toModality(ui));
        });
        var publications = java.util.Map.of(PublicationStatus.DRAFT, Models.EstadoPublicacion.BORRADOR,
                PublicationStatus.PUBLISHED, Models.EstadoPublicacion.PUBLICADA,
                PublicationStatus.PAUSED, Models.EstadoPublicacion.PAUSADA,
                PublicationStatus.CLOSED, Models.EstadoPublicacion.CERRADA,
                PublicationStatus.CANCELLED, Models.EstadoPublicacion.CANCELADA);
        publications.forEach((domain, ui) -> {
            assertEquals(ui, mapper.toEstadoPublicacion(domain));
            assertEquals(domain, mapper.toPublicationStatus(ui));
        });
        var bookings = java.util.Map.of(BookingStatus.PENDING, Models.EstadoContratacion.PENDIENTE,
                BookingStatus.ACCEPTED, Models.EstadoContratacion.ACEPTADA,
                BookingStatus.CONFIRMED, Models.EstadoContratacion.CONFIRMADA,
                BookingStatus.IN_PROGRESS, Models.EstadoContratacion.EN_CURSO,
                BookingStatus.COMPLETED, Models.EstadoContratacion.COMPLETADA,
                BookingStatus.REJECTED, Models.EstadoContratacion.RECHAZADA,
                BookingStatus.CANCELLED, Models.EstadoContratacion.CANCELADA);
        bookings.forEach((domain, ui) -> assertEquals(ui, mapper.toEstadoContratacion(domain)));
        assertEquals(Modality.values().length, modalities.size());
        assertEquals(PublicationStatus.values().length, publications.size());
        assertEquals(BookingStatus.values().length, bookings.size());
    }
}
