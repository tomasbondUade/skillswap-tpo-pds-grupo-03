package ar.edu.uade.skillswap.model;

import java.time.LocalDateTime;
import java.util.List;

public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL, HIBRIDA }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }

    public record Perfil(String id, String nombre, String rol, String bio,
                         List<String> habilidades, double reputacion,
                         int resenas, int proyectosCompletados) {}

    public record Publicacion(String id, String titulo, String descripcion,
                              String categoria, Modalidad modalidad, int duracionMinutos,
                              int precio, double reputacion, String proveedor,
                              List<String> etiquetas, EstadoPublicacion estado,
                              boolean destacada) {}

    public record Contratacion(String id, String titulo, String contraparte,
                               LocalDateTime fecha, int precio, EstadoContratacion estado) {}

    public record Movimiento(String concepto, LocalDateTime fecha, int importe, int saldo) {}

    public record Notificacion(String id, String titulo, String detalle,
                               LocalDateTime fecha, boolean leida, String tipo) {}

    public record FiltrosBusqueda(String texto, String categoria, Modalidad modalidad,
                                  int precioMaximo, String orden) {}
}
