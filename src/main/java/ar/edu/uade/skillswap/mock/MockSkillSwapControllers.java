package ar.edu.uade.skillswap.mock;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class MockSkillSwapControllers implements UiControllers,
        UiControllers.BusquedaController, UiControllers.PublicacionController,
        UiControllers.ContratacionController, UiControllers.WalletController,
        UiControllers.NotificacionController, UiControllers.PerfilController,
        UiControllers.CalificacionController {

    private final List<Publicacion> publicaciones = new ArrayList<>();
    private final List<Contratacion> contrataciones = new ArrayList<>();
    private final List<Movimiento> movimientos = new ArrayList<>();
    private final List<Notificacion> notificaciones = new ArrayList<>();
    private final Map<String, Perfil> perfilesCreados = new HashMap<>();
    private final Map<String, String> autores = new HashMap<>();
    private final Map<String, String> franjasPropuestas = new HashMap<>();
    private final Map<String, String> proponentes = new HashMap<>();
    private int saldo = 2480;

    public MockSkillSwapControllers() {
        publicaciones.addAll(List.of(
            pub("p1", "Preparación para entrevistas técnicas", "Simulacro completo, feedback y plan personalizado para tu próxima entrevista.", "Tecnología", Modalidad.REMOTA, 60, 180, 4.9, "Sofía Méndez", List.of("Java", "Entrevistas", "Backend"), true),
            pub("p2", "Revisión profesional de portfolio", "Mejorá la narrativa y presentación de tus proyectos para destacarte.", "Diseño", Modalidad.REMOTA, 45, 120, 4.8, "Tomás Vidal", List.of("UX", "Portfolio", "Carrera"), true),
            pub("p3", "Arquitectura limpia aplicada", "Sesión práctica para ordenar dependencias y casos de uso en proyectos Java.", "Tecnología", Modalidad.HIBRIDA, 90, 260, 5.0, "Camila Torres", List.of("Clean Architecture", "SOLID", "Java"), true),
            pub("p4", "Fotografía de producto con celular", "Iluminación, encuadre y edición simple para catálogos y redes.", "Creatividad", Modalidad.PRESENCIAL, 75, 150, 4.7, "Nicolás Paz", List.of("Fotografía", "Contenido"), false),
            pub("p5", "Mentoría de liderazgo para equipos", "Herramientas concretas para conversaciones, feedback y delegación.", "Negocios", Modalidad.REMOTA, 60, 220, 4.9, "Laura Rossi", List.of("Liderazgo", "Equipos"), false),
            pub("p6", "Excel para decisiones de negocio", "Tablas dinámicas, escenarios y un dashboard listo para reutilizar.", "Productividad", Modalidad.REMOTA, 90, 140, 4.6, "Martín Aguirre", List.of("Excel", "Datos"), false)
        ));
        contrataciones.add(new Contratacion("c1", "Revisión profesional de portfolio", "Tomás Vidal", LocalDateTime.now().plusDays(2), 120, EstadoContratacion.CONFIRMADA));
        contrataciones.add(new Contratacion("c2", "Mentoría de liderazgo para equipos", "Laura Rossi", LocalDateTime.now().minusDays(4), 220, EstadoContratacion.COMPLETADA));
        movimientos.add(new Movimiento("Saldo inicial de bienvenida", LocalDateTime.now().minusDays(30), 2500, 2500));
        movimientos.add(new Movimiento("Reserva: Revisión de portfolio", LocalDateTime.now().minusDays(1), -120, 2380));
        movimientos.add(new Movimiento("Mentoría brindada: Java inicial", LocalDateTime.now().minusHours(5), 100, 2480));
        notificaciones.add(new Notificacion("n1", "Tu reserva fue confirmada", "Tomás confirmó la sesión para el miércoles a las 18:00.", LocalDateTime.now().minusMinutes(32), false, "RESERVA"));
        notificaciones.add(new Notificacion("n2", "Nueva calificación recibida", "Laura te calificó con 5 estrellas. ¡Gran trabajo!", LocalDateTime.now().minusHours(3), false, "REPUTACION"));
        notificaciones.add(new Notificacion("n3", "Publicación destacada", "Tu mentoría de Java apareció en las recomendaciones de esta semana.", LocalDateTime.now().minusDays(1), true, "PUBLICACION"));
    }

    private Publicacion pub(String id, String titulo, String descripcion, String categoria,
                            Modalidad modalidad, int duracion, int precio, double reputacion,
                            String proveedor, List<String> tags, boolean destacada) {
        LocalDateTime firstStart = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        List<FranjaDisponibilidad> franjas = List.of(
                new FranjaDisponibilidad(id + "-slot-1", firstStart, firstStart.plusMinutes(duracion), 1),
                new FranjaDisponibilidad(id + "-slot-2", firstStart.plusDays(2),
                        firstStart.plusDays(2).plusMinutes(duracion), 1));
        return new Publicacion(id, titulo, descripcion, categoria, modalidad, duracion, precio,
                reputacion, proveedor, tags, EstadoPublicacion.PUBLICADA, destacada, franjas);
    }

    @Override public BusquedaController busqueda() { return this; }
    @Override public PublicacionController publicaciones() { return this; }
    @Override public ContratacionController contrataciones() { return this; }
    @Override public WalletController wallet() { return this; }
    @Override public NotificacionController notificaciones() { return this; }
    @Override public PerfilController perfiles() { return this; }
    @Override public CalificacionController calificaciones() { return this; }
    @Override public Calificacion calificar(String userId, String id, int score, String comment) {
        if (score < 1 || score > 5) throw new IllegalArgumentException("Puntaje fuera de rango");
        if (findContract(id).estado() != EstadoContratacion.COMPLETADA) throw new IllegalStateException("La contratación no terminó");
        return new Calificacion(UUID.randomUUID().toString(), id, score, comment, LocalDateTime.now());
    }

    @Override public List<Publicacion> buscar(FiltrosBusqueda f) {
        String text = f.texto() == null ? "" : f.texto().toLowerCase(Locale.ROOT).trim();
        return publicaciones.stream()
                .filter(p -> p.estado() == EstadoPublicacion.PUBLICADA)
                .filter(p -> text.isBlank() || (p.titulo()+p.descripcion()+String.join(" ", p.etiquetas())).toLowerCase(Locale.ROOT).contains(text))
                .filter(p -> f.categoria() == null || f.categoria().equals("Todas") || p.categoria().equals(f.categoria()))
                .filter(p -> f.modalidad() == null || p.modalidad() == f.modalidad())
                .filter(p -> f.precioMaximo() <= 0 || p.precio() <= f.precioMaximo())
                .sorted(comparator(f.orden())).collect(Collectors.toList());
    }

    private Comparator<Publicacion> comparator(String orden) {
        if ("Menor precio".equals(orden)) return Comparator.comparingInt(Publicacion::precio);
        if ("Mejor reputación".equals(orden)) return Comparator.comparingDouble(Publicacion::reputacion).reversed();
        return Comparator.comparing(Publicacion::destacada, Comparator.reverseOrder())
                .thenComparing(Publicacion::reputacion, Comparator.reverseOrder());
    }

    @Override public List<Publicacion> recomendar(String userId) { return buscar(new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).stream().limit(3).toList(); }
    @Override public List<String> categorias() { return List.of("Todas", "Tecnología", "Diseño", "Creatividad", "Negocios", "Productividad"); }
    @Override public List<Publicacion> listarPropias(String userId) {
        return publicaciones.stream().filter(p -> userId.equals(autores.get(p.id()))).toList();
    }

    @Override public Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria, Modalidad modalidad, int duracion, int precio) {
        Publicacion p = new Publicacion(UUID.randomUUID().toString(), titulo, descripcion, categoria, modalidad, duracion, precio, 0, "Mateo Silva", List.of(categoria), EstadoPublicacion.BORRADOR, false);
        publicaciones.add(p); autores.put(p.id(), userId); return p;
    }

    @Override public Publicacion crearBorrador(String userId, DatosBorrador datos) {
        Objects.requireNonNull(datos, "Faltan los datos del borrador");
        var slots = datos.disponibilidad().stream().map(slot -> new FranjaDisponibilidad(
                slot.id() == null ? UUID.randomUUID().toString() : slot.id(),
                slot.inicio(), slot.fin(), slot.cupos())).toList();
        var publication = new Publicacion(UUID.randomUUID().toString(), datos.titulo(), datos.descripcion(),
                datos.categoria(), datos.modalidad(), datos.duracionMinutos(), datos.precio(), 0,
                userId, datos.etiquetas(), EstadoPublicacion.BORRADOR, false, slots);
        publicaciones.add(publication);
        autores.put(publication.id(), userId);
        return publication;
    }

    @Override public Publicacion editarBorrador(String userId, String publicacionId, EdicionBorrador datos) {
        Objects.requireNonNull(datos, "Faltan los datos para editar el borrador");
        int index = indexOfPublication(publicacionId);
        Publicacion previous = publicaciones.get(index);
        if (!userId.equals(autores.get(publicacionId))) throw new SecurityException("No sos el autor");
        if (previous.estado() != EstadoPublicacion.BORRADOR) throw new IllegalStateException("Solo se editan borradores");
        var updated = new Publicacion(previous.id(), datos.titulo(), datos.descripcion(), datos.categoria(),
                datos.modalidad(), datos.duracionMinutos(), previous.precio(), previous.reputacion(),
                previous.proveedor(), datos.etiquetas(), previous.estado(), previous.destacada(),
                previous.franjasDisponibles());
        publicaciones.set(index, updated);
        return updated;
    }

    @Override public Perfil crearPerfil(String userId, NuevoPerfil datos) {
        Objects.requireNonNull(datos, "Faltan los datos del perfil");
        if (perfilesCreados.containsKey(userId) || "user-mateo".equals(userId)) {
            throw new IllegalStateException("El perfil ya existe");
        }
        var profile = new Perfil(userId, datos.nombre(), "Miembro de SkillSwap", datos.bio(),
                datos.habilidades(), 0, 0, 0);
        perfilesCreados.put(userId, profile);
        return profile;
    }

    @Override public Publicacion cambiarEstado(String userId, String id, EstadoPublicacion estado) {
        for (int i=0;i<publicaciones.size();i++) if (publicaciones.get(i).id().equals(id)) {
            Publicacion p=publicaciones.get(i); Publicacion n=new Publicacion(p.id(),p.titulo(),p.descripcion(),p.categoria(),p.modalidad(),p.duracionMinutos(),p.precio(),p.reputacion(),p.proveedor(),p.etiquetas(),estado,p.destacada(),p.franjasDisponibles()); publicaciones.set(i,n); return n;
        }
        throw new IllegalArgumentException("Publicación inexistente");
    }

    @Override public List<Contratacion> listarContrataciones(String userId) { return List.copyOf(contrataciones); }
    @Override public Contratacion solicitar(String userId, String publicacionId, String mensajeCliente,
                                            String objetivo, String franjaId) {
        if (objetivo == null || objetivo.isBlank()) throw new IllegalArgumentException("El objetivo de la mentoría es obligatorio");
        if (franjaId == null || franjaId.isBlank()) throw new IllegalArgumentException("Debe seleccionar una franja disponible");

        int publicationIndex = indexOfPublication(publicacionId);
        Publicacion publication = publicaciones.get(publicationIndex);
        if (publication.estado() != EstadoPublicacion.PUBLICADA) {
            throw new IllegalStateException("Solo se pueden solicitar publicaciones activas");
        }
        FranjaDisponibilidad selectedSlot = publication.franjasDisponibles().stream()
                .filter(slot -> slot.id().equals(franjaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La franja no pertenece a la publicación"));
        if (!selectedSlot.disponible()) throw new IllegalStateException("La franja ya no tiene cupos disponibles");

        String normalizedMessage = mensajeCliente == null ? "" : mensajeCliente.trim();
        Contratacion c = new Contratacion(UUID.randomUUID().toString(), publication.titulo(),
                publication.proveedor(), selectedSlot.inicio(), publication.precio(),
                EstadoContratacion.PENDIENTE, objetivo.trim(), normalizedMessage, selectedSlot.id());
        contrataciones.add(0,c);
        notificaciones.add(0,new Notificacion(UUID.randomUUID().toString(),"Solicitud enviada", "Tu solicitud para “"+publication.titulo()+"” fue enviada.",LocalDateTime.now(),false,"RESERVA")); return c;
    }

    private int indexOfPublication(String publicationId) {
        for (int index = 0; index < publicaciones.size(); index++) {
            if (publicaciones.get(index).id().equals(publicationId)) return index;
        }
        throw new IllegalArgumentException("Publicación inexistente");
    }

    @Override public Contratacion avanzar(String userId, String id) { return updateContract(id, false); }
    @Override public Contratacion cancelar(String userId, String id) { return updateContract(id, true); }
    private Contratacion updateContract(String id, boolean cancel) {
        for(int i=0;i<contrataciones.size();i++) if(contrataciones.get(i).id().equals(id)) {
            Contratacion c=contrataciones.get(i); EstadoContratacion next=cancel?EstadoContratacion.CANCELADA:switch(c.estado()) {case PENDIENTE->EstadoContratacion.ACEPTADA; case ACEPTADA->EstadoContratacion.CONFIRMADA; case CONFIRMADA->EstadoContratacion.EN_CURSO; case EN_CURSO->EstadoContratacion.COMPLETADA; default->c.estado();};
            Contratacion n=new Contratacion(c.id(),c.titulo(),c.contraparte(),c.fecha(),c.precio(),next,
                    c.objetivo(),c.mensajeCliente(),c.franjaId()); contrataciones.set(i,n); return n;
        } throw new IllegalArgumentException("Contratación inexistente");
    }


    // Comportamiento de demostración: las reglas reales se ejecutan en Booking/BookingUseCases.
    @Override public Contratacion aceptar(String userId, String id) {
        return transition(id, EstadoContratacion.PENDIENTE, EstadoContratacion.ACEPTADA);
    }

    @Override public Contratacion rechazar(String userId, String id, String motivo) {
        required(motivo, "Falta el motivo del rechazo");
        return transition(id, EstadoContratacion.PENDIENTE, EstadoContratacion.RECHAZADA);
    }

    @Override public Contratacion pagar(String userId, String id) {
        return transition(id, EstadoContratacion.ACEPTADA, EstadoContratacion.CONFIRMADA);
    }

    @Override public Contratacion cancelar(String userId, String id, String motivo) {
        required(motivo, "Falta el motivo de la cancelación");
        return cancelar(userId, id);
    }

    @Override public Contratacion proponerReprogramacion(String userId, String id, String nuevaFranjaId) {
        required(nuevaFranjaId, "Falta la nueva franja");
        Contratacion actual = findContract(id);
        if (actual.estado() != EstadoContratacion.ACEPTADA && actual.estado() != EstadoContratacion.CONFIRMADA) {
            throw new IllegalStateException("Solo se puede reprogramar antes de iniciar");
        }
        if (nuevaFranjaId.equals(actual.franjaId())) {
            throw new IllegalArgumentException("La franja debe ser distinta");
        }
        if (franjasPropuestas.containsKey(id)) {
            throw new IllegalStateException("Ya hay una propuesta pendiente");
        }
        franjasPropuestas.put(id, nuevaFranjaId);
        proponentes.put(id, userId);
        return actual;
    }

    @Override public Contratacion aceptarReprogramacion(String userId, String id) {
        Contratacion actual = findContract(id);
        String nuevaFranja = franjasPropuestas.get(id);
        if (nuevaFranja == null) throw new IllegalStateException("No hay una propuesta pendiente");
        if (Objects.equals(userId, proponentes.get(id))) {
            throw new SecurityException("La otra parte debe aceptar la propuesta");
        }
        Contratacion updated = new Contratacion(actual.id(), actual.titulo(), actual.contraparte(),
                actual.fecha(), actual.precio(), actual.estado(), actual.objetivo(),
                actual.mensajeCliente(), nuevaFranja);
        contrataciones.set(contrataciones.indexOf(actual), updated);
        franjasPropuestas.remove(id);
        proponentes.remove(id);
        return updated;
    }

    @Override public Contratacion iniciar(String userId, String id) {
        return transition(id, EstadoContratacion.CONFIRMADA, EstadoContratacion.EN_CURSO);
    }

    @Override public Contratacion completar(String userId, String id) {
        return transition(id, EstadoContratacion.EN_CURSO, EstadoContratacion.COMPLETADA);
    }

    @Override public boolean puedeAvanzar(String userId, String id) {
        EstadoContratacion estado = findContract(id).estado();
        return estado != EstadoContratacion.COMPLETADA
                && estado != EstadoContratacion.RECHAZADA
                && estado != EstadoContratacion.CANCELADA;
    }

    private Contratacion transition(String id, EstadoContratacion expected, EstadoContratacion target) {
        Contratacion actual = findContract(id);
        if (actual.estado() != expected) throw new IllegalStateException("Transición no permitida");
        Contratacion updated = new Contratacion(actual.id(), actual.titulo(), actual.contraparte(),
                actual.fecha(), actual.precio(), target, actual.objetivo(),
                actual.mensajeCliente(), actual.franjaId());
        contrataciones.set(contrataciones.indexOf(actual), updated);
        return updated;
    }

    private Contratacion findContract(String id) {
        return contrataciones.stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Contratación inexistente"));
    }

    private static void required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    @Override public int consultarSaldo(String userId) { return saldo; }
    @Override public List<Movimiento> listarMovimientos(String userId) { List<Movimiento> copia=new ArrayList<>(movimientos); Collections.reverse(copia); return List.copyOf(copia); }
    @Override public int acreditarDemo(String userId, int importe) { saldo+=importe; movimientos.add(new Movimiento("Créditos promocionales",LocalDateTime.now(),importe,saldo)); return saldo; }
    @Override public List<Notificacion> listarNotificaciones(String userId) { return List.copyOf(notificaciones); }
    @Override public void marcarLeida(String userId, String id) { for(int i=0;i<notificaciones.size();i++){Notificacion n=notificaciones.get(i);if(n.id().equals(id))notificaciones.set(i,new Notificacion(n.id(),n.titulo(),n.detalle(),n.fecha(),true,n.tipo()));} }
    @Override public void marcarTodasLeidas(String userId) { for(Notificacion n:List.copyOf(notificaciones)) marcarLeida(userId,n.id()); }
    @Override public Perfil verPerfil(String userId) {
        if (perfilesCreados.containsKey(userId)) return perfilesCreados.get(userId);
        return new Perfil(userId,"Mateo Silva","Software Engineer & Mentor","Construyo productos digitales y acompaño a personas que quieren crecer en tecnología.",List.of("Java","Clean Architecture","Mentoría","SQL","Diseño OO"),4.9,37,52); }
}
