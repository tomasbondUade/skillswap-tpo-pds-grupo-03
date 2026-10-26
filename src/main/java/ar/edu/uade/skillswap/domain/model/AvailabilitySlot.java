package ar.edu.uade.skillswap.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Franja horaria reservable con capacidad acotada. */
public final class AvailabilitySlot {
    private final String id;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final int capacity;
    private int reserved;

    public AvailabilitySlot(String id, LocalDateTime startsAt, LocalDateTime endsAt, int capacity, int reserved) {
        this.id = id == null ? UUID.randomUUID().toString() : required(id, "Falta identificador de franja");
        this.startsAt = Objects.requireNonNull(startsAt);
        this.endsAt = Objects.requireNonNull(endsAt);
        if (!endsAt.isAfter(startsAt)) throw new IllegalArgumentException("La franja debe terminar después de comenzar");
        if (capacity < 1 || reserved < 0 || reserved > capacity) throw new IllegalArgumentException("Capacidad inválida");
        this.capacity = capacity;
        this.reserved = reserved;
    }

    public static AvailabilitySlot of(LocalDateTime startsAt, LocalDateTime endsAt, int capacity) {
        return new AvailabilitySlot(null, startsAt, endsAt, capacity, 0);
    }

    void reserve() {
        if (reserved >= capacity) throw new IllegalStateException("La franja ya no tiene cupo");
        reserved++;
    }

    void release() {
        if (reserved == 0) throw new IllegalStateException("No hay reservas para liberar");
        reserved--;
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public String id() { return id; }
    public LocalDateTime startsAt() { return startsAt; }
    public LocalDateTime endsAt() { return endsAt; }
    public int capacity() { return capacity; }
    public int reserved() { return reserved; }
    public int remaining() { return capacity - reserved; }
}
