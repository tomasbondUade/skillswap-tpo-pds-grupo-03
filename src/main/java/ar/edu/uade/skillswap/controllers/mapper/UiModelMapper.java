package ar.edu.uade.skillswap.controllers.mapper;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.model.WalletData.MovementView;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.model.Models;

/** Converts application results to the simple data models used by the Swing UI. */
public final class UiModelMapper {
    public SearchQuery toSearchQuery(Models.FiltrosBusqueda filtros) {
        if (filtros == null) throw new IllegalArgumentException("Faltan los filtros de búsqueda");
        return new SearchQuery(filtros.texto(),
                filtros.categoria() != null && !"Todas".equals(filtros.categoria())
                        ? filtros.categoria() : null,
                filtros.modalidad() == null ? null : toModality(filtros.modalidad()),
                filtros.habilidad(), filtros.precioMinimo(), filtros.precioMaximo(),
                filtros.reputacionMinima(), toSearchOrder(filtros.orden()),
                filtros.pagina(), filtros.tamanio());
    }

    public Models.PaginaPublicaciones toPaginaPublicaciones(PageResult<PublicationView> page) {
        return new Models.PaginaPublicaciones(page.items().stream().map(this::toPublicacion).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public Models.Publicacion toPublicacion(PublicationView view) {
        return new Models.Publicacion(view.id(), view.title(), view.description(), view.category(),
                toModalidad(view.modality()), view.durationMinutes(), view.priceInSkillCoins(),
                view.author().reputation(), view.author().displayName(), view.tags().stream().toList(),
                toEstadoPublicacion(view.status()),
                false, view.availability().stream()
                    .map(slot -> new Models.FranjaDisponibilidad(slot.id(), slot.startsAt(), slot.endsAt(),
                            slot.capacity() - slot.reserved()))
                    .toList(), toTipoPublicacion(view.type()));
    }

    public Models.TipoPublicacion toTipoPublicacion(PublicationType type) {
        return switch (type) {
            case MENTORSHIP -> Models.TipoPublicacion.MENTORIA;
            case SERVICE -> Models.TipoPublicacion.SERVICIO;
            case CHALLENGE -> Models.TipoPublicacion.DESAFIO;
        };
    }

    public PublicationType toPublicationType(Models.TipoPublicacion tipo) {
        if (tipo == null) throw new IllegalArgumentException("Falta el tipo de publicación");
        return switch (tipo) {
            case MENTORIA -> PublicationType.MENTORSHIP;
            case SERVICIO -> PublicationType.SERVICE;
            case DESAFIO -> PublicationType.CHALLENGE;
        };
    }

    public Models.Contratacion toContratacion(BookingView view, String titulo, String contraparte) {
        return new Models.Contratacion(view.id(), titulo, contraparte, view.createdAt(),
                view.priceInSkillCoins(), toEstadoContratacion(view.status()), view.objective(),
                view.clientMessage(), view.slotId());
    }

    public Models.Perfil toPerfil(ProfileView view, String rol, int proyectosCompletados) {
        return new Models.Perfil(view.userId(), view.displayName(), rol, view.biography(),
                view.skills(), view.reputation(), view.ratingCount(), proyectosCompletados);
    }

    public Models.Movimiento toMovimiento(MovementView view) {
        return new Models.Movimiento(view.concept(), view.occurredAt(), view.amountInSkillCoins(),
                view.resultingBalanceInSkillCoins());
    }

    public Models.Notificacion toNotificacion(NotificationView view) {
        return new Models.Notificacion(view.id(), view.title(), view.detail(), view.createdAt(),
                view.read(), view.type());
    }

    public Modality toModality(Models.Modalidad modalidad) {
        if (modalidad == null) throw new IllegalArgumentException("Falta la modalidad");
        return switch (modalidad) {
            case REMOTA -> Modality.REMOTE;
            case PRESENCIAL -> Modality.IN_PERSON;
            case HIBRIDA -> Modality.HYBRID;
        };
    }

    public Models.Modalidad toModalidad(Modality modality) {
        return switch (modality) {
            case REMOTE -> Models.Modalidad.REMOTA;
            case IN_PERSON -> Models.Modalidad.PRESENCIAL;
            case HYBRID -> Models.Modalidad.HIBRIDA;
        };
    }

    public PublicationStatus toPublicationStatus(Models.EstadoPublicacion estado) {
        if (estado == null) throw new IllegalArgumentException("Falta el estado de la publicación");
        return switch (estado) {
            case BORRADOR -> PublicationStatus.DRAFT;
            case PUBLICADA -> PublicationStatus.PUBLISHED;
            case PAUSADA -> PublicationStatus.PAUSED;
            case CERRADA -> PublicationStatus.CLOSED;
            case CANCELADA -> PublicationStatus.CANCELLED;
        };
    }

    public Models.EstadoPublicacion toEstadoPublicacion(PublicationStatus status) {
        return switch (status) {
            case DRAFT -> Models.EstadoPublicacion.BORRADOR;
            case PUBLISHED -> Models.EstadoPublicacion.PUBLICADA;
            case PAUSED -> Models.EstadoPublicacion.PAUSADA;
            case CLOSED -> Models.EstadoPublicacion.CERRADA;
            case CANCELLED -> Models.EstadoPublicacion.CANCELADA;
        };
    }

    public Models.EstadoContratacion toEstadoContratacion(BookingStatus status) {
        return switch (status) {
            case PENDING -> Models.EstadoContratacion.PENDIENTE;
            case ACCEPTED -> Models.EstadoContratacion.ACEPTADA;
            case CONFIRMED -> Models.EstadoContratacion.CONFIRMADA;
            case IN_PROGRESS -> Models.EstadoContratacion.EN_CURSO;
            case COMPLETED -> Models.EstadoContratacion.COMPLETADA;
            case REJECTED -> Models.EstadoContratacion.RECHAZADA;
            case CANCELLED -> Models.EstadoContratacion.CANCELADA;
        };
    }

    private SearchOrder toSearchOrder(String orden) {
        if (orden == null || orden.isBlank() || "Relevancia".equals(orden)) return SearchOrder.RELEVANCE;
        return switch (orden) {
            case "Mejor reputación" -> SearchOrder.BEST_RATING;
            case "Menor precio" -> SearchOrder.LOWEST_PRICE;
            case "Más recientes" -> SearchOrder.MOST_RECENT;
            default -> throw new IllegalArgumentException("Orden de búsqueda desconocido: " + orden);
        };
    }
}
