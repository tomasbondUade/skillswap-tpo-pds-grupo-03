package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.state.PublicationState;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Agregado de publicación: protege sus invariantes, cupos y transiciones de estado. */
public final class Publication {
    private final String id;
    private final String authorUserId;
    private String title;
    private String description;
    private String category;
    private Modality modality;
    private int durationMinutes;
    private int capacity;
    private final SkillCoins basePrice;
    private final Set<String> tags;
    private final List<AvailabilitySlot> availability;
    private final List<StatusHistoryEntry> history = new ArrayList<>();
    private PublicationState state;
    private final LocalDateTime createdAt;
    private int requestCount;
    private LocalDateTime lastRequestedAt;
    private long version;

    /** Constructor de reconstitución del agregado desde una capa externa. */
    public Publication(String id, String authorUserId, String title, String description, String category,
                       Modality modality, int durationMinutes, int capacity, SkillCoins basePrice,
                       Collection<String> tags, Collection<AvailabilitySlot> availability,
                       PublicationState state, LocalDateTime createdAt, int requestCount,
                       LocalDateTime lastRequestedAt, long version, Collection<StatusHistoryEntry> history) {
        this.id = id == null ? UUID.randomUUID().toString() : required(id, "Falta identificador de publicación");
        this.authorUserId = required(authorUserId, "Falta autor de publicación");
        this.title = normalized(title);
        this.description = normalized(description);
        this.category = normalized(category);
        this.modality = Objects.requireNonNull(modality, "Falta modalidad");
        requirePositive(durationMinutes, "La duración debe ser positiva");
        requirePositive(capacity, "El cupo debe ser positivo");
        this.durationMinutes = durationMinutes;
        this.capacity = capacity;
        this.basePrice = Objects.requireNonNull(basePrice, "Falta precio base");
        this.tags = normalizedTags(tags);
        this.availability = new ArrayList<>(availability == null ? List.of() : availability);
        validateAvailability(this.availability, durationMinutes, capacity);
        this.state = state == null ? PublicationStates.draft() : state;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        if (requestCount < 0 || version < 0) throw new IllegalArgumentException("El contador o la versión no pueden ser negativos");
        this.requestCount = requestCount;
        this.lastRequestedAt = lastRequestedAt;
        if (requestCount == 0 && lastRequestedAt != null) {
            throw new IllegalArgumentException("Una publicación sin solicitudes no puede registrar actividad de solicitud");
        }
        this.version = version;
        if (history != null) {
            for (StatusHistoryEntry entry : history) this.history.add(Objects.requireNonNull(entry));
        }
    }

    /** Crea un borrador; completar campos obligatorios sigue siendo requisito para publicarlo. */
    public static Publication draft(String author, String title, String description, String category,
                                    Modality modality, int duration, int capacity,
                                    SkillCoins price, Collection<String> tags) {
        LocalDateTime createdAt = LocalDateTime.now();
        Publication publication = new Publication(null, author, title, description, category, modality,
                duration, capacity, price, tags, List.of(), PublicationStates.draft(), createdAt,
                0, null, 0, List.of());
        publication.history.add(new StatusHistoryEntry(null, PublicationStatus.DRAFT.name(),
                author, "Borrador creado", createdAt));
        return publication;
    }

    public void edit(String actor, String title, String description, String category, Modality modality,
                     int durationMinutes, int capacity, Collection<String> tags) {
        requireOwner(actor);
        requireDraft("Solo se puede editar un borrador");
        Objects.requireNonNull(modality, "Falta modalidad");
        requirePositive(durationMinutes, "La duración debe ser positiva");
        requirePositive(capacity, "El cupo debe ser positivo");
        Set<String> updatedTags = normalizedTags(tags);
        validateAvailability(availability, durationMinutes, capacity);

        this.title = normalized(title);
        this.description = normalized(description);
        this.category = normalized(category);
        this.modality = modality;
        this.durationMinutes = durationMinutes;
        this.capacity = capacity;
        this.tags.clear();
        this.tags.addAll(updatedTags);
    }

    public void addAvailability(String actor, AvailabilitySlot slot) {
        requireOwner(actor);
        requireDraft("La disponibilidad solo se modifica en borradores");
        AvailabilitySlot newSlot = Objects.requireNonNull(slot, "Falta la franja");
        List<AvailabilitySlot> candidate = new ArrayList<>(availability);
        candidate.add(newSlot);
        validateAvailability(candidate, durationMinutes, capacity);
        availability.add(newSlot);
    }

    public void publish(String actor) {
        requireOwner(actor);
        transition(state.publish(this), actor, "Publicada");
    }

    public void pause(String actor) {
        requireOwner(actor);
        transition(state.pause(this), actor, "Pausada");
    }

    public void close(String actor) {
        requireOwner(actor);
        transition(state.close(this), actor, "Cerrada");
    }

    public void cancel(String actor, String reason) {
        requireOwner(actor);
        String cancellationReason = required(reason, "La cancelación requiere un motivo");
        transition(state.cancel(this, cancellationReason), actor, cancellationReason);
    }

    /** Valida y registra una nueva solicitud sin reservar todavía el cupo. */
    public void recordBookingRequest(String requesterUserId, String slotId) {
        String requester = required(requesterUserId, "Falta quien solicita la publicación");
        if (authorUserId.equals(requester)) throw new IllegalArgumentException("El autor no puede solicitar su propia publicación");
        if (!state.acceptsRequests()) throw new IllegalStateException("La publicación no admite solicitudes");
        AvailabilitySlot selectedSlot = slot(slotId);
        if (selectedSlot.remaining() <= 0) throw new IllegalStateException("La franja ya no tiene cupo");
        requestCount = Math.incrementExact(requestCount);
        lastRequestedAt = LocalDateTime.now();
    }

    public void reserve(String slotId) {
        if (!state.acceptsRequests()) throw new IllegalStateException("La publicación no admite reservas");
        slot(slotId).reserve();
    }

    public void releaseReservation(String slotId) {
        slot(slotId).release();
    }

    public AvailabilitySlot firstAvailableSlot() {
        if (!state.acceptsRequests()) throw new IllegalStateException("La publicación no admite solicitudes");
        return availability.stream().filter(slot -> slot.remaining() > 0).findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay franjas disponibles"));
    }

    /** Invariante consultada por el State al pasar de borrador/pausada a publicada. */
    public void validateBeforePublishing() {
        if (title.isBlank() || description.isBlank() || category.isBlank()) {
            throw new IllegalStateException("Título, descripción y categoría son obligatorios");
        }
        if (availability.isEmpty()) throw new IllegalStateException("Publicar requiere al menos una franja de disponibilidad");
        validateAvailability(availability, durationMinutes, capacity);
    }

    public static void requireCancellationReason(String reason) {
        required(reason, "La cancelación requiere un motivo");
    }

    private AvailabilitySlot slot(String slotId) {
        String requestedId = required(slotId, "Debe indicar una franja");
        return availability.stream().filter(candidate -> candidate.id().equals(requestedId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La franja no pertenece a esta publicación"));
    }

    private void transition(PublicationState next, String actor, String reason) {
        Objects.requireNonNull(next, "Falta el estado destino");
        history.add(new StatusHistoryEntry(state.status().name(), next.status().name(),
                actor, reason, LocalDateTime.now()));
        state = next;
    }

    private void requireOwner(String actor) {
        if (!authorUserId.equals(actor)) throw new SecurityException("Solo el autor puede modificar la publicación");
    }

    private void requireDraft(String message) {
        if (state.status() != PublicationStatus.DRAFT) throw new IllegalStateException(message);
    }

    private static void validateAvailability(List<AvailabilitySlot> slots, int durationMinutes, int publicationCapacity) {
        Set<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < slots.size(); index++) {
            AvailabilitySlot slot = Objects.requireNonNull(slots.get(index), "La disponibilidad no puede contener nulos");
            if (!ids.add(slot.id())) throw new IllegalArgumentException("La franja no puede repetirse");
            if (slot.capacity() > publicationCapacity) {
                throw new IllegalArgumentException("El cupo de la franja supera el cupo de la publicación");
            }
            if (Duration.between(slot.startsAt(), slot.endsAt()).toMinutes() < durationMinutes) {
                throw new IllegalArgumentException("La franja es más corta que la duración de la sesión");
            }
            for (int otherIndex = index + 1; otherIndex < slots.size(); otherIndex++) {
                AvailabilitySlot other = Objects.requireNonNull(slots.get(otherIndex), "La disponibilidad no puede contener nulos");
                if (slot.startsAt().isBefore(other.endsAt()) && other.startsAt().isBefore(slot.endsAt())) {
                    throw new IllegalArgumentException("Las franjas disponibles no pueden superponerse");
                }
            }
        }
    }

    private static Set<String> normalizedTags(Collection<String> tags) {
        Set<String> normalized = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) normalized.add(required(tag, "Las etiquetas no pueden estar vacías"));
        }
        return normalized;
    }

    private static String normalized(String value) { return value == null ? "" : value.trim(); }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private static void requirePositive(int value, String message) {
        if (value <= 0) throw new IllegalArgumentException(message);
    }

    public void restoreVersion(long version) {
        if (version < 0) throw new IllegalArgumentException("La versión no puede ser negativa");
        this.version = version;
    }

    public String id() { return id; }
    public String authorUserId() { return authorUserId; }
    public String title() { return title; }
    public String description() { return description; }
    public String category() { return category; }
    public Modality modality() { return modality; }
    public int durationMinutes() { return durationMinutes; }
    public int capacity() { return capacity; }
    public SkillCoins basePrice() { return basePrice; }
    public Set<String> tags() { return Set.copyOf(tags); }
    public List<AvailabilitySlot> availability() { return List.copyOf(availability); }
    public PublicationStatus status() { return state.status(); }
    public boolean acceptsRequests() { return state.acceptsRequests(); }
    public List<StatusHistoryEntry> history() { return List.copyOf(history); }
    public LocalDateTime createdAt() { return createdAt; }
    public int requestCount() { return requestCount; }
    public LocalDateTime lastRequestedAt() { return lastRequestedAt; }
    public long version() { return version; }
}
