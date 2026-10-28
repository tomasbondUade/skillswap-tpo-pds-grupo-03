package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.DiscoveryData;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.BookingData.CancelCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.ProfileData.AvailabilityInput;
import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.model.WalletData.WalletView;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.model.Models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.UUID;

public final class WiredUiControllers implements UiControllers,
        UiControllers.BusquedaController,
        UiControllers.PublicacionController,
        UiControllers.ContratacionController,
        UiControllers.WalletController,
        UiControllers.NotificacionController,
        UiControllers.PerfilController {

    private final SkillSwapApplicationWiring wiring = new SkillSwapApplicationWiring();
    private final Map<String, Models.Publicacion> publicationCache = new HashMap<>();

    @Override
    public BusquedaController busqueda() {
        return this;
    }

    @Override
    public PublicacionController publicaciones() {
        return this;
    }

    @Override
    public ContratacionController contrataciones() {
        return this;
    }

    @Override
    public WalletController wallet() {
        return this;
    }

    @Override
    public NotificacionController notificaciones() {
        return this;
    }

    @Override
    public PerfilController perfiles() {
        return this;
    }

    @Override
    public List<Models.Publicacion> buscar(Models.FiltrosBusqueda filtros) {
        var query = new SearchQuery(
                filtros.texto(),
                filtros.categoria() != null && !"Todas".equals(filtros.categoria()) ? filtros.categoria() : null,
                filtros.modalidad() == null ? null : mapModality(filtros.modalidad()),
                null,
                filtros.precioMaximo() > 0 ? filtros.precioMaximo() : null,
                null,
                mapOrder(filtros.orden()),
                0,
                20
        );

        return wiring.discoveryService().search(query).items().stream()
                .map(this::toPublicacion)
                .toList();
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        return wiring.discoveryService().recommend(3).stream()
                .map(this::toPublicacion)
                .toList();
    }

    @Override
    public List<String> categorias() {
        return java.util.stream.Stream.concat(
            java.util.stream.Stream.of("Todas"),
            wiring.discoveryService().categories().stream())
            .toList();
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return wiring.publicationService().listMyPublications().stream()
                .map(this::toPublicacion)
                .toList();
    }

    @Override
    public Models.Publicacion crearBorrador(String userId, String titulo, String descripcion,
                                           String categoria, Models.Modalidad modalidad,
                                           int duracion, int precio) {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        var command = new CreateDraftCommand(
                titulo,
                descripcion,
                categoria,
                mapModality(modalidad),
                duracion,
                6,
                precio,
                Set.of(),
                List.of(new AvailabilityInput(UUID.randomUUID().toString(), startsAt,
                    startsAt.plusMinutes(duracion), 6))
        );
        return toPublicacion(wiring.publicationService().createDraft(command));
    }

    @Override
    public Models.Publicacion cambiarEstado(String userId, String publicacionId, Models.EstadoPublicacion estado) {
        var command = new ChangeStatusCommand(
                publicacionId,
                mapStatus(estado),
                "Cambio ejecutado desde la UI"
        );
        return toPublicacion(wiring.publicationService().changeStatus(command));
    }

    @Override
    public Models.Perfil verPerfil(String userId) {
        return toPerfil(wiring.profileService().viewMyProfile());
    }

    @Override
    public List<Models.Contratacion> listarContrataciones(String userId) {
        return wiring.bookingService().listMyBookings().stream()
                .map(this::toContratacion)
                .toList();
    }

    @Override
    public Models.Contratacion solicitar(String userId, String publicacionId, String mensajeCliente,
                                         String objetivo, String franjaId) {
        if (!publicationCache.containsKey(publicacionId)) {
            buscar(new Models.FiltrosBusqueda("", "Todas", null, 0, "Relevancia"))
                .stream()
                .filter(item -> item.id().equals(publicacionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Publicación inexistente o no disponible"));
        }
        BookingView booking = wiring.bookingService().request(
            new RequestCommand(publicacionId, mensajeCliente, objetivo, franjaId));
        return toContratacion(booking);
    }

    @Override
    public Models.Contratacion avanzar(String userId, String contratacionId) {
        BookingView booking = findBooking(contratacionId);
        BookingView updated = switch (booking.status()) {
            case PENDING -> wiring.bookingService().accept(new BookingCommand(contratacionId));
            case ACCEPTED -> wiring.bookingService().pay(new BookingCommand(contratacionId));
            case CONFIRMED -> wiring.bookingService().start(new BookingCommand(contratacionId));
            case IN_PROGRESS -> wiring.bookingService().complete(new BookingCommand(contratacionId));
            default -> booking;
        };
        return toContratacion(updated);
    }

    @Override
    public Models.Contratacion cancelar(String userId, String contratacionId) {
        return toContratacion(wiring.bookingService().cancel(
            new CancelCommand(contratacionId, "Cancelada desde la interfaz")));
    }

    @Override
    public boolean puedeAvanzar(String userId, String contratacionId) {
        BookingView booking = findBooking(contratacionId);
        return switch (booking.status()) {
            case PENDING -> booking.providerUserId().equals(userId);
            case ACCEPTED -> booking.buyerUserId().equals(userId);
            case CONFIRMED, IN_PROGRESS -> booking.buyerUserId().equals(userId)
                    || booking.providerUserId().equals(userId);
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }

    @Override
    public int consultarSaldo(String userId) {
        return wiring.walletService().viewMyWallet().balanceInSkillCoins();
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return wiring.walletService().viewMyWallet().movements().stream()
            .map(movement -> new Models.Movimiento(movement.concept(), movement.occurredAt(),
                movement.amountInSkillCoins(), movement.resultingBalanceInSkillCoins()))
            .toList();
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return wiring.walletService().creditDemo(importe).balanceInSkillCoins();
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return wiring.notificationService().listMine().stream()
            .map(notification -> new Models.Notificacion(notification.id(), notification.title(),
                notification.detail(), notification.createdAt(), notification.read(), notification.type()))
            .toList();
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        wiring.notificationService().markRead(notificacionId);
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        wiring.notificationService().markAllRead();
    }

    private Models.Publicacion toPublicacion(PublicationView view) {
        Models.Publicacion publication = new Models.Publicacion(
                view.id(),
                view.title(),
                view.description(),
                view.category(),
                mapModality(view.modality()),
                view.durationMinutes(),
                view.priceInSkillCoins(),
                reputationFor(view),
                providerName(view.authorUserId()),
                view.tags().stream().toList(),
                mapStatus(view.status()),
                false,
                view.availability().stream()
                    .map(slot -> new Models.FranjaDisponibilidad(slot.id(), slot.startsAt(), slot.endsAt(),
                        slot.capacity() - slot.reserved()))
                    .toList()
        );
        publicationCache.put(view.id(), publication);
        return publication;
    }

    private Models.Contratacion toContratacion(BookingView booking) {
        Models.Publicacion publication = publicationCache.get(booking.publicationId());
        String title = publication == null ? booking.publicationId() : publication.titulo();
        String counterpart = "user-mateo".equals(booking.buyerUserId())
                ? providerName(booking.providerUserId())
                : "Mateo Silva";
        return new Models.Contratacion(booking.id(), title, counterpart, booking.createdAt(),
            booking.priceInSkillCoins(), mapBookingStatus(booking.status()), booking.objective(),
            booking.clientMessage(), booking.slotId());
    }

    private BookingView findBooking(String bookingId) {
        return wiring.bookingService().listMyBookings().stream()
                .filter(booking -> booking.id().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Contratación inexistente"));
    }

    private static Models.EstadoContratacion mapBookingStatus(BookingStatus status) {
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

    private Models.Perfil toPerfil(ProfileView view) {
        return new Models.Perfil(
                view.userId(),
                view.displayName(),
                "Software Engineer & Mentor",
                view.biography(),
                view.skills(),
                view.reputation(),
                view.ratingCount(),
                52
        );
    }

    private static double reputationFor(PublicationView view) {
        if ("user-mateo".equals(view.authorUserId())) {
            return 4.9;
        }
        return 4.8 + ((view.requestCount() % 3) * 0.1);
    }

    private static String providerName(String authorUserId) {
        return "user-mateo".equals(authorUserId) ? "Mateo Silva" : "Mentor SkillSwap";
    }

    private static Modality mapModality(Models.Modalidad modalidad) {
        return switch (modalidad) {
            case REMOTA -> Modality.REMOTE;
            case PRESENCIAL -> Modality.IN_PERSON;
            case HIBRIDA -> Modality.HYBRID;
        };
    }

    private static Models.Modalidad mapModality(Modality modalidad) {
        return switch (modalidad) {
            case REMOTE -> Models.Modalidad.REMOTA;
            case IN_PERSON -> Models.Modalidad.PRESENCIAL;
            case HYBRID -> Models.Modalidad.HIBRIDA;
        };
    }

    private static DiscoveryData.SearchOrder mapOrder(String orden) {
        if (orden == null) {
            return DiscoveryData.SearchOrder.RELEVANCE;
        }
        if ("Mejor reputación".equals(orden)) {
            return DiscoveryData.SearchOrder.BEST_RATING;
        }
        if ("Menor precio".equals(orden)) {
            return DiscoveryData.SearchOrder.LOWEST_PRICE;
        }
        return DiscoveryData.SearchOrder.RELEVANCE;
    }

    private static PublicationStatus mapStatus(Models.EstadoPublicacion estado) {
        return switch (estado) {
            case BORRADOR -> PublicationStatus.DRAFT;
            case PUBLICADA -> PublicationStatus.PUBLISHED;
            case PAUSADA -> PublicationStatus.PAUSED;
            case CERRADA -> PublicationStatus.CLOSED;
            case CANCELADA -> PublicationStatus.CANCELLED;
        };
    }

    private static Models.EstadoPublicacion mapStatus(PublicationStatus estado) {
        return switch (estado) {
            case DRAFT -> Models.EstadoPublicacion.BORRADOR;
            case PUBLISHED -> Models.EstadoPublicacion.PUBLICADA;
            case PAUSED -> Models.EstadoPublicacion.PAUSADA;
            case CLOSED -> Models.EstadoPublicacion.CERRADA;
            case CANCELLED -> Models.EstadoPublicacion.CANCELADA;
        };
    }
}