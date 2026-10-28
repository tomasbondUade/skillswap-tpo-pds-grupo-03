package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.*;
import java.util.List;

/**
 * UI contracts: success results contain only simple view models.
 * Real implementations translate failures to PresentationException, whose error()
 * contains the stable presentation code and the user-facing message.
 */
public interface UiControllers {
    interface BusquedaController {
        List<Publicacion> buscar(FiltrosBusqueda filtros);
        List<Publicacion> recomendar(String userId);
        List<String> categorias();
    }

    interface PublicacionController {
        List<Publicacion> listarPropias(String userId);
        Publicacion crearBorrador(String userId, DatosBorrador datos);
        Publicacion crearBorrador(String userId, String titulo, String descripcion,
                                  String categoria, Modalidad modalidad, int duracion, int precio);
        Publicacion editarBorrador(String userId, String publicacionId, EdicionBorrador datos);
        Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado);
    }

    interface ContratacionController {
        List<Contratacion> listarContrataciones(String userId);
        Contratacion solicitar(String userId, String publicacionId, String mensajeCliente,
                               String objetivo, String franjaId);
        Contratacion avanzar(String userId, String contratacionId);
        Contratacion cancelar(String userId, String contratacionId);
        default boolean puedeAvanzar(String userId, String contratacionId) { return true; }
    }

    interface WalletController {
        int consultarSaldo(String userId);
        List<Movimiento> listarMovimientos(String userId);
        int acreditarDemo(String userId, int importe);
    }

    interface NotificacionController {
        List<Notificacion> listarNotificaciones(String userId);
        void marcarLeida(String userId, String notificacionId);
        void marcarTodasLeidas(String userId);
    }

    interface PerfilController {
        Perfil crearPerfil(String userId, NuevoPerfil datos);
        Perfil verPerfil(String userId);
    }

    BusquedaController busqueda();
    PublicacionController publicaciones();
    ContratacionController contrataciones();
    WalletController wallet();
    NotificacionController notificaciones();
    PerfilController perfiles();
}
