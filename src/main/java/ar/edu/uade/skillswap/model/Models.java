package ar.edu.uade.skillswap.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class Models {
    private Models() {}

    public enum Modalidad { REMOTA, PRESENCIAL, HIBRIDA }
    public enum EstadoPublicacion { BORRADOR, PUBLICADA, PAUSADA, CERRADA, CANCELADA }

    /** Tipo de publicación; {@code toString()} es el texto que muestra la pantalla. */
    public enum TipoPublicacion {
        MENTORIA("Mentoría"), SERVICIO("Servicio"), DESAFIO("Desafío");

        private final String etiqueta;

        TipoPublicacion(String etiqueta) { this.etiqueta = etiqueta; }

        @Override public String toString() { return etiqueta; }
    }
    public enum EstadoContratacion { PENDIENTE, ACEPTADA, CONFIRMADA, EN_CURSO, COMPLETADA, RECHAZADA, CANCELADA }

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

    /** {@code tipo} es nulo cuando el origen de los datos no lo informa. */
    public record Publicacion(String id, String titulo, String descripcion,
                              String categoria, Modalidad modalidad, int duracionMinutos,
                              int precio, double reputacion, String proveedor,
                              List<String> etiquetas, EstadoPublicacion estado,
                              boolean destacada, List<FranjaDisponibilidad> franjasDisponibles,
                              TipoPublicacion tipo) {
        public Publicacion {
            etiquetas = List.copyOf(etiquetas == null ? List.of() : etiquetas);
            franjasDisponibles = List.copyOf(franjasDisponibles == null ? List.of() : franjasDisponibles);
        }

        public Publicacion(String id, String titulo, String descripcion, String categoria,
                           Modalidad modalidad, int duracionMinutos, int precio, double reputacion,
                           String proveedor, List<String> etiquetas, EstadoPublicacion estado,
                           boolean destacada, List<FranjaDisponibilidad> franjasDisponibles) {
            this(id, titulo, descripcion, categoria, modalidad, duracionMinutos, precio,
                    reputacion, proveedor, etiquetas, estado, destacada, franjasDisponibles, null);
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

    /**
     * Filtros de la pantalla Explorar. Los textos vacíos, "Todas" y los valores nulos significan
     * "sin filtro". El orden usa las etiquetas de la pantalla: "Relevancia", "Mejor reputación",
     * "Menor precio" o "Más recientes".
     */
    public record FiltrosBusqueda(String texto, String categoria, Modalidad modalidad, String habilidad,
                                  Integer precioMinimo, Integer precioMaximo, Double reputacionMinima,
                                  String orden, int pagina, int tamanio) {
        public static final int TAMANIO_PAGINA = 20;

        /** Firma usada por la UI provista: sin filtros nuevos, primera página. */
        public FiltrosBusqueda(String texto, String categoria, Modalidad modalidad, int precioMaximo, String orden) {
            this(texto, categoria, modalidad, null, null, precioMaximo > 0 ? precioMaximo : null, null,
                    orden, 0, TAMANIO_PAGINA);
        }
    }

    /** Página de resultados de búsqueda; {@code pagina} empieza en cero. */
    public record PaginaPublicaciones(List<Publicacion> items, int pagina, int tamanio,
                                      long total, int totalPaginas) {
        public PaginaPublicaciones {
            items = List.copyOf(items == null ? List.of() : items);
        }

        public boolean hayAnterior() { return pagina > 0; }
        public boolean haySiguiente() { return pagina + 1 < totalPaginas; }
    }
}
