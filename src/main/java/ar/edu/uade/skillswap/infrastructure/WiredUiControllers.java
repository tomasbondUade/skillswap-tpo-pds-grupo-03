package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.BookingData.CancelCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.PerfilUiController;
import ar.edu.uade.skillswap.controllers.PublicacionUiController;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WiredUiControllers implements UiControllers,
        UiControllers.BusquedaController,
        UiControllers.PublicacionController,
        UiControllers.ContratacionController,
        UiControllers.WalletController,
        UiControllers.NotificacionController,
        UiControllers.PerfilController {

    private final SkillSwapApplicationWiring wiring;
    private final PerfilUiController profileController;
    private final PublicacionUiController publicationController;
    private final UiModelMapper mapper = new UiModelMapper();
    private final UiOperationExecutor operations = new UiOperationExecutor();
    private final Map<String, Models.Publicacion> publicationCache = new HashMap<>();

    public WiredUiControllers() {
        this(new SkillSwapApplicationWiring());
    }

    public WiredUiControllers(SkillSwapApplicationWiring wiring) {
        this.wiring = java.util.Objects.requireNonNull(wiring);
        profileController = new PerfilUiController(wiring.profileService(), wiring.currentUserProvider(), operations);
        publicationController = new PublicacionUiController(wiring.publicationService(), wiring.currentUserProvider(), operations);
    }

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
        return operations.execute(() -> {
            var query = mapper.toSearchQuery(filtros);

            return wiring.discoveryService().search(query).items().stream()
                    .map(this::toPublicacion)
                    .toList();
        });
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        return operations.execute(() -> {
            return wiring.discoveryService().recommend(3).stream()
                    .map(this::toPublicacion)
                    .toList();
        });
    }

    @Override
    public List<String> categorias() {
        return operations.execute(() -> {
            return java.util.stream.Stream.concat(
                java.util.stream.Stream.of("Todas"),
                wiring.discoveryService().categories().stream())
                .toList();
        });
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return publicationController.listarPropias(userId).stream().map(this::remember).toList();
    }

    @Override
    public Models.Publicacion crearBorrador(String userId, String titulo, String descripcion,
                                           String categoria, Models.Modalidad modalidad,
                                           int duracion, int precio) {
        return remember(publicationController.crearBorrador(userId, titulo, descripcion,
                categoria, modalidad, duracion, precio));
    }

    @Override
    public Models.Publicacion crearBorrador(String userId, Models.DatosBorrador datos) {
        return remember(publicationController.crearBorrador(userId, datos));
    }

    @Override
    public Models.Publicacion editarBorrador(String userId, String publicacionId, Models.EdicionBorrador datos) {
        return remember(publicationController.editarBorrador(userId, publicacionId, datos));
    }

    @Override
    public Models.Publicacion cambiarEstado(String userId, String publicacionId, Models.EstadoPublicacion estado) {
        return remember(publicationController.cambiarEstado(userId, publicacionId, estado));
    }

    @Override
    public Models.Perfil crearPerfil(String userId, Models.NuevoPerfil datos) {
        return profileController.crearPerfil(userId, datos);
    }

    @Override
    public Models.Perfil verPerfil(String userId) {
        return profileController.verPerfil(userId);
    }

    private Models.Publicacion remember(Models.Publicacion publication) {
        publicationCache.put(publication.id(), publication);
        return publication;
    }

    @Override
    public List<Models.Contratacion> listarContrataciones(String userId) {
        return operations.execute(() -> {
            return wiring.bookingService().listMyBookings().stream()
                    .map(this::toContratacion)
                    .toList();
        });
    }

    @Override
    public Models.Contratacion solicitar(String userId, String publicacionId, String mensajeCliente,
                                         String objetivo, String franjaId) {
        return operations.execute(() -> {
            if (!publicationCache.containsKey(publicacionId)) {
                buscar(new Models.FiltrosBusqueda("", "Todas", null, 0, "Relevancia"))
                    .stream()
                    .filter(item -> item.id().equals(publicacionId))
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Publicación inexistente o no disponible"));
            }
            BookingView booking = wiring.bookingService().request(
                new RequestCommand(publicacionId, mensajeCliente, objetivo, franjaId));
            return toContratacion(booking);
        });
    }

    @Override
    public Models.Contratacion avanzar(String userId, String contratacionId) {
        return operations.execute(() -> {
            BookingView booking = findBooking(contratacionId);
            BookingView updated = switch (booking.status()) {
                case PENDING -> wiring.bookingService().accept(new BookingCommand(contratacionId));
                case ACCEPTED -> wiring.bookingService().pay(new BookingCommand(contratacionId));
                case CONFIRMED -> wiring.bookingService().start(new BookingCommand(contratacionId));
                case IN_PROGRESS -> wiring.bookingService().complete(new BookingCommand(contratacionId));
                default -> booking;
            };
            return toContratacion(updated);
        });
    }

    @Override
    public Models.Contratacion cancelar(String userId, String contratacionId) {
        return operations.execute(() -> {
            return toContratacion(wiring.bookingService().cancel(
                new CancelCommand(contratacionId, "Cancelada desde la interfaz")));
        });
    }

    @Override
    public boolean puedeAvanzar(String userId, String contratacionId) {
        return operations.execute(() -> {
            BookingView booking = findBooking(contratacionId);
            return switch (booking.status()) {
                case PENDING -> booking.providerUserId().equals(userId);
                case ACCEPTED -> booking.buyerUserId().equals(userId);
                case CONFIRMED, IN_PROGRESS -> booking.buyerUserId().equals(userId)
                        || booking.providerUserId().equals(userId);
                case COMPLETED, REJECTED, CANCELLED -> false;
            };
        });
    }

    @Override
    public int consultarSaldo(String userId) {
        return operations.execute(() -> {
            return wiring.walletService().viewMyWallet().balanceInSkillCoins();
        });
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return operations.execute(() -> {
            return wiring.walletService().viewMyWallet().movements().stream()
                .map(mapper::toMovimiento)
                .toList();
        });
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return operations.execute(() -> {
            return wiring.walletService().creditDemo(importe).balanceInSkillCoins();
        });
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return operations.execute(() -> {
            return wiring.notificationService().listMine().stream()
                .map(mapper::toNotificacion)
                .toList();
        });
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        operations.execute(() -> {
            wiring.notificationService().markRead(notificacionId);
        });
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        operations.execute(() -> {
            wiring.notificationService().markAllRead();
        });
    }

    private Models.Publicacion toPublicacion(PublicationView view) {
        Models.Publicacion publication = mapper.toPublicacion(view,
                providerName(view.authorUserId()), reputationFor(view));
        publicationCache.put(view.id(), publication);
        return publication;
    }

    private Models.Contratacion toContratacion(BookingView booking) {
        Models.Publicacion publication = publicationCache.get(booking.publicationId());
        String title = publication == null ? booking.publicationId() : publication.titulo();
        String counterpart = "user-mateo".equals(booking.buyerUserId())
                ? providerName(booking.providerUserId())
                : "Mateo Silva";
        return mapper.toContratacion(booking, title, counterpart);
    }

    private BookingView findBooking(String bookingId) {
        return wiring.bookingService().listMyBookings().stream()
                .filter(booking -> booking.id().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Contratación inexistente"));
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

}
