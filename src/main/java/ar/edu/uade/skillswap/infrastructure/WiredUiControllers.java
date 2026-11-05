package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.controllers.ContratacionUiController;
import ar.edu.uade.skillswap.controllers.CalificacionUiController;
import ar.edu.uade.skillswap.controllers.BusquedaUiController;
import ar.edu.uade.skillswap.controllers.PerfilUiController;
import ar.edu.uade.skillswap.controllers.PublicacionUiController;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WiredUiControllers implements UiControllers,
        UiControllers.BusquedaController,
        UiControllers.PublicacionController,
        UiControllers.WalletController,
        UiControllers.NotificacionController,
        UiControllers.PerfilController,
        UiControllers.SesionDemoController {

    private final SkillSwapApplicationWiring wiring;
    private final PerfilUiController profileController;
    private final CalificacionUiController ratingController;
    private final BusquedaUiController searchController;
    private final ContratacionUiController bookingController;
    private final PublicacionUiController publicationController;

    private final UiModelMapper mapper = new UiModelMapper();
    private final UiOperationExecutor operations = new UiOperationExecutor();
    private final Map<String, Models.Publicacion> publicationCache = new HashMap<>();

    public WiredUiControllers() {
        this(new SkillSwapApplicationWiring());
    }

    public WiredUiControllers(SkillSwapApplicationWiring wiring) {
        this.wiring = java.util.Objects.requireNonNull(wiring);
        searchController = new BusquedaUiController(wiring.discoveryService(), wiring.currentUserProvider(), operations);
        profileController = new PerfilUiController(wiring.profileService(), wiring.currentUserProvider(), operations);
        ratingController = new CalificacionUiController(wiring.ratingService(), wiring.currentUserProvider(), operations);
        bookingController = new ContratacionUiController(wiring.bookingService(),
                wiring.currentUserProvider(), operations, this::toContratacion);
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
        return bookingController;
    }

    @Override public CalificacionController calificaciones() { return ratingController; }
    @Override public SesionDemoController sesionDemo() { return this; }
    @Override public String usuarioActual() { return wiring.currentUserProvider().currentUserId(); }
    @Override public void cambiarUsuario(String userId) {
        operations.execute(() -> {
            ((DemoCurrentUserProvider) wiring.currentUserProvider()).switchTo(userId);
            publicationCache.clear();
        });
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
    public Models.PaginaPublicaciones buscar(Models.FiltrosBusqueda filtros) {
        Models.PaginaPublicaciones page = searchController.buscar(filtros);
        page.items().forEach(this::remember);
        return page;
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        return searchController.recomendar(userId).stream().map(this::remember).toList();
    }

    @Override
    public List<String> categorias() {
        return searchController.categorias();
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return publicationController.listarPropias(userId).stream()
                .map(this::remember)
                .toList();
    }

    @Override
    public Models.Publicacion crearBorrador(
            String userId,
            String titulo,
            String descripcion,
            String categoria,
            Models.Modalidad modalidad,
            int duracion,
            int precio) {
        return remember(publicationController.crearBorrador(
                userId, titulo, descripcion, categoria, modalidad, duracion, precio));
    }

    @Override
    public Models.Publicacion crearBorrador(
            String userId,
            Models.DatosBorrador datos) {
        return remember(publicationController.crearBorrador(userId, datos));
    }

    @Override
    public Models.Publicacion editarBorrador(
            String userId,
            String publicacionId,
            Models.EdicionBorrador datos) {
        return remember(publicationController.editarBorrador(
                userId, publicacionId, datos));
    }

    @Override
    public Models.Publicacion cambiarEstado(
            String userId,
            String publicacionId,
            Models.EstadoPublicacion estado) {
        return remember(publicationController.cambiarEstado(
                userId, publicacionId, estado));
    }

    @Override
    public Models.Perfil crearPerfil(
            String userId,
            Models.NuevoPerfil datos) {
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
    public int consultarSaldo(String userId) {
        return operations.execute(() ->
                wiring.walletService().viewMyWallet().balanceInSkillCoins());
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return operations.execute(() ->
                wiring.walletService().viewMyWallet().movements().stream()
                        .map(mapper::toMovimiento)
                        .toList());
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return operations.execute(() ->
                wiring.walletService().creditDemo(importe).balanceInSkillCoins());
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return operations.execute(() ->
                wiring.notificationService().listMine().stream()
                        .map(mapper::toNotificacion)
                        .toList());
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        operations.execute(() ->
                wiring.notificationService().markRead(notificacionId));
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        operations.execute(() ->
                wiring.notificationService().markAllRead());
    }

    private Models.Publicacion toPublicacion(PublicationView view) {
        Models.Publicacion publication = mapper.toPublicacion(view);
        publicationCache.put(view.id(), publication);
        return publication;
    }

    private Models.Contratacion toContratacion(BookingView booking) {
        Models.Publicacion publication =
                publicationCache.get(booking.publicationId());
        String title = publication == null
                ? booking.publicationId()
                : publication.titulo();
        String counterpart = "user-mateo".equals(booking.buyerUserId())
                ? providerName(booking.providerUserId())
                : "Mateo Silva";
        return mapper.toContratacion(booking, title, counterpart);
    }

    private String providerName(String authorUserId) {
        return wiring.discoveryService().author(authorUserId).displayName();
    }
}
