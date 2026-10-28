package ar.edu.uade.skillswap.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL, HIBRIDA }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }

    public record Perfil(String id, String nombre, String rol, String bio,
                         List<String> habilidades, double reputacion,
                         int resenas, int proyectosCompletados) {}

    public record FranjaDisponibilidad(String id, LocalDateTime inicio,
                                       LocalDateTime fin, int cuposDisponibles) {
        public FranjaDisponibilidad {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Falta identificador de franja");
            Objects.requireNonNull(inicio, "Falta el inicio de la franja");
            Objects.requireNonNull(fin, "Falta el fin de la franja");
            if (!inicio.isBefore(fin)) throw new IllegalArgumentException("El inicio debe ser anterior al fin");
            if (cuposDisponibles < 0) throw new IllegalArgumentException("Los cupos no pueden ser negativos");
        }

        public boolean disponible() { return cuposDisponibles > 0; }
    }

    public record Publicacion(String id, String titulo, String descripcion,
                              String categoria, Modalidad modalidad, int duracionMinutos,
                              int precio, double reputacion, String proveedor,
                              List<String> etiquetas, EstadoPublicacion estado,
                              boolean destacada, List<FranjaDisponibilidad> franjasDisponibles) {
        public Publicacion {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
            franjasDisponibles = List.copyOf(franjasDisponibles == null ? List.of() : franjasDisponibles);
        }

        public Publicacion(String id, String titulo, String descripcion, String categoria,
                           Modalidad modalidad, int duracionMinutos, int precio, double reputacion,
                           String proveedor, List<String> etiquetas, EstadoPublicacion estado,
                           boolean destacada) {
            this(id, titulo, descripcion, categoria, modalidad, duracionMinutos, precio,
                    reputacion, proveedor, etiquetas, estado, destacada, List.of());
        }
    }

    public record Contratacion(String id, String titulo, String contraparte,
                               LocalDateTime fecha, int precio, EstadoContratacion estado,
                               String objetivo, String mensajeCliente, String franjaId) {
        public Contratacion(String id, String titulo, String contraparte, LocalDateTime fecha,
                            int precio, EstadoContratacion estado) {
            this(id, titulo, contraparte, fecha, precio, estado, "", "", "");
        }
    }

    public record Movimiento(String concepto, LocalDateTime fecha, int importe, int saldo) {}

    public record Notificacion(String id, String titulo, String detalle,
                               LocalDateTime fecha, boolean leida, String tipo) {}

    public record FiltrosBusqueda(String texto, String categoria, Modalidad modalidad,
                                  int precioMaximo, String orden) {}
}
