package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.state.BookingState;
import ar.edu.uade.skillswap.domain.state.BookingStates;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Agregado de contratación: autoriza a sus participantes y protege el ciclo de vida. */
public final class Booking {
    private final String id;
    private final String publicationId;
    private final String buyerUserId;
    private final String providerUserId;
    private final String objective;
    private final String clientMessage;
    private String slotId;
    private String proposedSlotId;
    private boolean buyerAcceptedReschedule;
    private boolean providerAcceptedReschedule;
    private final SkillCoins price;
    private SkillCoins refund;
    private BookingState state;
    private final LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private final List<StatusHistoryEntry> history = new ArrayList<>();

    /** Constructor de reconstitución para adapters de persistencia. */
    public Booking(String id, String publicationId, String buyerUserId, String providerUserId,
                   String objective, String slotId, SkillCoins price, BookingState state,
                   LocalDateTime createdAt, LocalDateTime paidAt, SkillCoins refund,
                   String proposedSlotId, boolean buyerAcceptedReschedule,
                   boolean providerAcceptedReschedule, Collection<StatusHistoryEntry> history) {
        this(id, publicationId, buyerUserId, providerUserId, objective, "", slotId, price,
                state, createdAt, paidAt, refund, proposedSlotId, buyerAcceptedReschedule,
                providerAcceptedReschedule, history);
    }

    /** Constructor de reconstitución que conserva el mensaje original del cliente. */
    public Booking(String id, String publicationId, String buyerUserId, String providerUserId,
                   String objective, String clientMessage, String slotId, SkillCoins price,
                   BookingState state, LocalDateTime createdAt, LocalDateTime paidAt,
                   SkillCoins refund, String proposedSlotId, boolean buyerAcceptedReschedule,
                   boolean providerAcceptedReschedule, Collection<StatusHistoryEntry> history) {
        this.id = id == null ? UUID.randomUUID().toString() : required(id, "Falta identificador de contratación");
        this.publicationId = required(publicationId, "Falta publicación");
        this.buyerUserId = required(buyerUserId, "Falta cliente");
        this.providerUserId = required(providerUserId, "Falta proveedor");
        if (this.buyerUserId.equals(this.providerUserId)) {
            throw new IllegalArgumentException("El autor no puede contratar su propia publicación");
        }
        this.objective = required(objective, "El objetivo es obligatorio");
        this.clientMessage = clientMessage == null ? "" : clientMessage.trim();
        this.slotId = required(slotId, "Debe elegirse una franja");
        this.price = Objects.requireNonNull(price, "Falta el precio de la contratación");
        this.state = Objects.requireNonNull(state, "Falta estado de contratación");
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.paidAt = paidAt;
        this.refund = refund == null ? SkillCoins.zero() : refund;
        if (this.refund.amount() > this.price.amount()) throw new IllegalArgumentException("El reintegro no puede superar el precio pagado");
        if (this.refund.amount() > 0 && (paidAt == null || this.state.status() != BookingStatus.CANCELLED)) {
            throw new IllegalArgumentException("El reintegro requiere una contratación cancelada y pagada");
        }
        boolean paymentRequired = this.state.status() == BookingStatus.CONFIRMED
                || this.state.status() == BookingStatus.IN_PROGRESS
                || this.state.status() == BookingStatus.COMPLETED;
        boolean paymentForbidden = this.state.status() == BookingStatus.PENDING
                || this.state.status() == BookingStatus.ACCEPTED
                || this.state.status() == BookingStatus.REJECTED;
        if ((paymentRequired && paidAt == null) || (paymentForbidden && paidAt != null)) {
            throw new IllegalArgumentException("La fecha de pago no coincide con el estado de la contratación");
        }
        this.proposedSlotId = proposedSlotId == null ? null : required(proposedSlotId, "La franja propuesta no puede estar vacía");
        if (this.slotId.equals(this.proposedSlotId)) throw new IllegalArgumentException("La nueva franja debe ser distinta de la actual");
        if (this.proposedSlotId == null && (buyerAcceptedReschedule || providerAcceptedReschedule)) {
            throw new IllegalArgumentException("No puede haber aceptaciones sin una reprogramación pendiente");
        }
        this.buyerAcceptedReschedule = buyerAcceptedReschedule;
        this.providerAcceptedReschedule = providerAcceptedReschedule;
        if (history != null) {
            for (StatusHistoryEntry entry : history) this.history.add(Objects.requireNonNull(entry));
        }
    }

    public static Booking request(Publication publication, String buyer, String objective,
                                  String slotId, SkillCoins price) {
        return request(publication, buyer, "", objective, slotId, price);
    }

    /** Crea una solicitud con mensaje, objetivo y franja elegida por quien solicita. */
    public static Booking request(Publication publication, String buyer, String clientMessage,
                                  String objective, String slotId, SkillCoins price) {
        Objects.requireNonNull(publication, "Falta publicación");
        String requester = required(buyer, "Falta cliente");
        String goal = required(objective, "El objetivo es obligatorio");
        String selectedSlot = required(slotId, "Debe elegirse una franja");
        SkillCoins agreedPrice = Objects.requireNonNull(price, "Falta el precio de la contratación");
        publication.recordBookingRequest(requester, selectedSlot);
        LocalDateTime requestedAt = LocalDateTime.now();
        Booking booking = new Booking(null, publication.id(), requester, publication.authorUserId(), goal,
                clientMessage, selectedSlot, agreedPrice, BookingStates.pending(), requestedAt, null,
                SkillCoins.zero(), null, false, false, List.of());
        booking.history.add(new StatusHistoryEntry(null, BookingStatus.PENDING.name(),
                requester, "Solicitud creada", requestedAt));
        return booking;
    }

    public void accept(String actor) {
        requireActor(actor, providerUserId);
        transition(state.accept(this), actor, "Solicitud aceptada");
    }

    public void reject(String actor, String reason) {
        requireActor(actor, providerUserId);
        transition(state.reject(this), actor, required(reason, "El rechazo requiere un motivo"));
    }

    public void confirmPayment(String actor) {
        assertPaymentAllowed(actor);
        transition(state.pay(this), actor, "Pago confirmado");
        paidAt = LocalDateTime.now();
    }

    public void assertPaymentAllowed(String actor) {
        requireActor(actor, buyerUserId);
        state.pay(this);
    }

    public void start(String actor) {
        requireParticipant(actor);
        transition(state.start(this), actor, "Sesión iniciada");
    }

    public void complete(String actor) {
        requireParticipant(actor);
        transition(state.complete(this), actor, "Sesión completada");
    }

    public void cancel(String actor, String reason, SkillCoins refund) {
        requireParticipant(actor);
        SkillCoins acceptedRefund = Objects.requireNonNull(refund, "Falta calcular el reintegro");
        if (acceptedRefund.amount() > price.amount()) throw new IllegalArgumentException("El reintegro no puede superar el precio pagado");
        if (paidAt == null && acceptedRefund.amount() > 0) {
            throw new IllegalArgumentException("No se puede reintegrar una contratación que no fue pagada");
        }
        transition(state.cancel(this), actor, required(reason, "La cancelación requiere un motivo"));
        this.refund = acceptedRefund;
    }

    public void proposeReschedule(String actor, String newSlotId) {
        requireParticipant(actor);
        if (state.status() != BookingStatus.ACCEPTED && state.status() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Solo se puede reprogramar antes de comenzar");
        }
        String requestedSlot = required(newSlotId, "Debe indicar la nueva franja");
        if (slotId.equals(requestedSlot)) throw new IllegalArgumentException("La nueva franja debe ser distinta de la actual");
        if (proposedSlotId != null && !proposedSlotId.equals(requestedSlot)) {
            throw new IllegalStateException("Ya existe una propuesta de reprogramación pendiente");
        }
        proposedSlotId = requestedSlot;
        buyerAcceptedReschedule = actor.equals(buyerUserId);
        providerAcceptedReschedule = actor.equals(providerUserId);
    }

    /** Confirma la propuesta cuando el otro participante también la acepta. */
    public boolean acceptReschedule(String actor) {
        requireParticipant(actor);
        if (proposedSlotId == null) throw new IllegalStateException("No hay una reprogramación propuesta");
        if (actor.equals(buyerUserId)) buyerAcceptedReschedule = true;
        else providerAcceptedReschedule = true;
        if (!buyerAcceptedReschedule || !providerAcceptedReschedule) return false;

        slotId = proposedSlotId;
        proposedSlotId = null;
        buyerAcceptedReschedule = false;
        providerAcceptedReschedule = false;
        return true;
    }

    private void transition(BookingState next, String actor, String reason) {
        Objects.requireNonNull(next, "Falta el estado destino");
        history.add(new StatusHistoryEntry(state.status().name(), next.status().name(), actor, reason, LocalDateTime.now()));
        state = next;
    }

    private void requireParticipant(String actor) {
        if (!buyerUserId.equals(actor) && !providerUserId.equals(actor)) {
            throw new SecurityException("Solo quienes participan pueden operar esta contratación");
        }
    }

    private void requireActor(String actor, String expected) {
        if (!expected.equals(actor)) throw new SecurityException("El usuario no tiene permiso para esta operación");
    }

    private static String required(String value, String error) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(error);
        return value.trim();
    }

    public String id() { return id; }
    public String publicationId() { return publicationId; }
    public String buyerUserId() { return buyerUserId; }
    public String providerUserId() { return providerUserId; }
    public String objective() { return objective; }
    public String clientMessage() { return clientMessage; }
    public String slotId() { return slotId; }
    public String proposedSlotId() { return proposedSlotId; }
    public SkillCoins price() { return price; }
    public boolean buyerAcceptedReschedule() { return buyerAcceptedReschedule; }
    public boolean providerAcceptedReschedule() { return providerAcceptedReschedule; }
    public SkillCoins refund() { return refund; }
    public BookingStatus status() { return state.status(); }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime paidAt() { return paidAt; }
    public List<StatusHistoryEntry> history() { return List.copyOf(history); }
}
