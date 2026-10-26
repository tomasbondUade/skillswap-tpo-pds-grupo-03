package ar.edu.uade.skillswap.application.model;

import java.time.LocalDateTime;

public final class RatingData {
    private RatingData() {}

    public record SubmitCommand(String bookingId, int score, String comment) {}
    public record RatingView(String id, String bookingId, String authorUserId,
                             String subjectUserId, int score, String comment,
                             LocalDateTime createdAt) {}
}
