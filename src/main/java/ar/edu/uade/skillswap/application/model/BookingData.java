package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.BookingStatus;
import java.time.LocalDateTime;

/** Input and output models for a booking workflow. */
public final class BookingData {
    private BookingData() {}

    public record RequestCommand(String publicationId, String objective, String slotId) {}
    public record BookingCommand(String bookingId) {}
    public record RejectCommand(String bookingId, String reason) {}
    public record CancelCommand(String bookingId, String reason) {}
    public record RescheduleCommand(String bookingId, String newSlotId) {}

    public record BookingView(String id, String publicationId, String buyerUserId,
                              String providerUserId, String objective, String slotId,
                              int priceInSkillCoins, int refundInSkillCoins,
                              BookingStatus status, LocalDateTime createdAt, LocalDateTime paidAt) {}
}
