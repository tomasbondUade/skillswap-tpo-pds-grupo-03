package ar.edu.uade.skillswap.controllers.mapper;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.model.WalletData.MovementView;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.model.Models;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityInput;
import ar.edu.uade.skillswap.application.model.ProfileData.RegisterCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.EditDraftCommand;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import java.util.Set;
import java.util.stream.Collectors;

/** Converts application results to the simple data models used by the Swing UI. */
public final class UiModelMapper {
    public RegisterCommand toRegisterCommand(Models.NuevoPerfil datos) {
        if (datos == null) throw new IllegalArgumentException("Faltan los datos del perfil");
        return new RegisterCommand(datos.nombre(), datos.bio(), datos.zonaHoraria(),
                datos.habilidades(), datos.intereses(), datos.canales().stream()
                    .map(this::toNotificationChannel).collect(Collectors.toSet()),
                datos.disponibilidad().stream().map(this::toAvailabilityInput).toList());
    }

    public CreateDraftCommand toCreateDraftCommand(Models.DatosBorrador datos) {
        if (datos == null) throw new IllegalArgumentException("Faltan los datos del borrador");
        return new CreateDraftCommand(datos.titulo(), datos.descripcion(), datos.categoria(),
                toModality(datos.modalidad()), datos.duracionMinutos(), datos.cupos(), datos.precio(),
                Set.copyOf(datos.etiquetas()),
                datos.disponibilidad().stream().map(this::toAvailabilityInput).toList());
    }

    public EditDraftCommand toEditDraftCommand(String publicacionId, Models.EdicionBorrador datos) {
        if (datos == null) throw new IllegalArgumentException("Faltan los datos para editar el borrador");
        return new EditDraftCommand(publicacionId, datos.titulo(), datos.descripcion(), datos.categoria(),
                toModality(datos.modalidad()), datos.duracionMinutos(), datos.cupos(), Set.copyOf(datos.etiquetas()));
    }

    private AvailabilityInput toAvailabilityInput(Models.Disponibilidad slot) {
        return new AvailabilityInput(slot.id(), slot.inicio(), slot.fin(), slot.cupos());
    }

    private NotificationChannelType toNotificationChannel(Models.CanalNotificacion canal) {
        return switch (canal) {
            case EMAIL -> NotificationChannelType.EMAIL;
            case PUSH -> NotificationChannelType.PUSH;
            case IN_APP -> NotificationChannelType.IN_APP;
        };
    }

    public SearchQuery toSearchQuery(Models.FiltrosBusqueda filtros) {
        if (filtros == null) throw new IllegalArgumentException("Faltan los filtros de búsqueda");
        return new SearchQuery(filtros.texto(),
                filtros.categoria() != null && !"Todas".equals(filtros.categoria())
                        ? filtros.categoria() : null,
                filtros.modalidad() == null ? null : toModality(filtros.modalidad()),
                null, filtros.precioMaximo() > 0 ? filtros.precioMaximo() : null,
                null, toSearchOrder(filtros.orden()), 0, 20);
    }

    public Models.Publicacion toPublicacion(PublicationView view, String proveedor, double reputacion) {
        return new Models.Publicacion(view.id(), view.title(), view.description(), view.category(),
                toModalidad(view.modality()), view.durationMinutes(), view.priceInSkillCoins(),
                reputacion, proveedor, view.tags().stream().toList(), toEstadoPublicacion(view.status()),
                false, view.availability().stream()
                    .map(slot -> new Models.FranjaDisponibilidad(slot.id(), slot.startsAt(), slot.endsAt(),
                            slot.capacity() - slot.reserved()))
                    .toList());
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
        if ("Mejor reputación".equals(orden)) return SearchOrder.BEST_RATING;
        if ("Menor precio".equals(orden)) return SearchOrder.LOWEST_PRICE;
        return SearchOrder.RELEVANCE;
    }
}
