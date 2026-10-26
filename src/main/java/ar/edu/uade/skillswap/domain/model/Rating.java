package ar.edu.uade.skillswap.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Calificación inmutable, creada a partir de una contratación completada. */
public final class Rating {
    private final String id;
    private final String bookingId;
    private final String authorUserId;
    private final String subjectUserId;
    private final int score;
    private final String comment;
    private final LocalDateTime createdAt;

    private Rating(String id, String bookingId, String authorUserId, String subjectUserId,
                   int score, String comment, LocalDateTime createdAt) {
        this.id = id == null ? UUID.randomUUID().toString() : required(id, "Falta identificador de calificación");
        this.bookingId = required(bookingId, "Falta la contratación");
        this.authorUserId = required(authorUserId, "Falta quien califica");
        this.subjectUserId = required(subjectUserId, "Falta quien recibe la calificación");
        if (this.authorUserId.equals(this.subjectUserId)) throw new IllegalArgumentException("No podés calificarte a vos mismo");
        if (score < 1 || score > 5) throw new IllegalArgumentException("La calificación debe estar entre 1 y 5");
        this.score = score;
        this.comment = comment == null ? "" : comment.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "Falta la fecha de la calificación");
    }

    /** Autoriza y construye una calificación de un participante sobre la contraparte. */
    public static Rating forCompletedBooking(Booking booking, String authorUserId, int score, String comment) {
        Objects.requireNonNull(booking, "Falta contratación");
        String author = required(authorUserId, "Falta quien califica");
        if (booking.status() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Solo se puede calificar una contratación completada");
        }
        String subject;
        if (booking.buyerUserId().equals(author)) subject = booking.providerUserId();
        else if (booking.providerUserId().equals(author)) subject = booking.buyerUserId();
        else throw new SecurityException("Solo un participante puede calificar esta contratación");
        return new Rating(null, booking.id(), author, subject, score, comment, LocalDateTime.now());
    }

    /** Reconstituye un registro ya persistido; no se usa para crear calificaciones nuevas. */
    public static Rating restore(String id, String bookingId, String authorUserId, String subjectUserId,
                                 int score, String comment, LocalDateTime createdAt) {
        return new Rating(id, bookingId, authorUserId, subjectUserId, score, comment, createdAt);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public String id() { return id; }
    public String bookingId() { return bookingId; }
    public String authorUserId() { return authorUserId; }
    public String subjectUserId() { return subjectUserId; }
    public int score() { return score; }
    public String comment() { return comment; }
    public LocalDateTime createdAt() { return createdAt; }
}
