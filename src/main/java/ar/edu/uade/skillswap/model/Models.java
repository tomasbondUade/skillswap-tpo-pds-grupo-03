package ar.edu.uade.skillswap.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL, HIBRIDA }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }
    public enum CanalNotificacion { EMAIL, PUSH, IN_APP }

    public record Disponibilidad(String id, LocalDateTime inicio, LocalDateTime fin, int cupos) {}

    public record NuevoPerfil(String nombre, String bio, String zonaHoraria,
                              List<String> habilidades, List<String> intereses,
                              Set<CanalNotificacion> canales, List<Disponibilidad> disponibilidad) {
        public NuevoPerfil {
            habilidades = List.copyOf(habilidades == null ? List.of() : habilidades);
            intereses = List.copyOf(intereses == null ? List.of() : intereses);
            canales = Set.copyOf(canales == null ? Set.of() : canales);
            disponibilidad = List.copyOf(disponibilidad == null ? List.of() : disponibilidad);
        }
    }

    public record DatosBorrador(String titulo, String descripcion, String categoria,
                                Modalidad modalidad, int duracionMinutos, int cupos, int precio,
                                List<String> etiquetas, List<Disponibilidad> disponibilidad) {
        public DatosBorrador {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
            disponibilidad = List.copyOf(disponibilidad == null ? List.of() : disponibilidad);
        }
    }

    public record EdicionBorrador(String titulo, String descripcion, String categoria,
                                 Modalidad modalidad, int duracionMinutos, int cupos,
                                 List<String> etiquetas) {
        public EdicionBorrador {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
        }
    }

    public enum CodigoError {
        VALIDACION, NO_AUTENTICADO, NO_ENCONTRADO, PERMISOS, ESTADO,
        CONFLICTO, PERSISTENCIA, INESPERADO
    }

    public record ErrorPresentacion(CodigoError codigo, String mensaje) {
        public ErrorPresentacion {
            Objects.requireNonNull(codigo, "Falta el código del error");
            if (mensaje == null || mensaje.isBlank()) {
                throw new IllegalArgumentException("Falta el mensaje del error");
            }
        }
    }

    public record Perfil(String id, String nombre, String rol, String bio,
                         List<String> habilidades, double reputacion,
                         int resenas, int proyectosCompletados) {
        public Perfil {
            habilidades = List.copyOf(habilidades == null ? List.of() : habilidades);
        }
    }

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
