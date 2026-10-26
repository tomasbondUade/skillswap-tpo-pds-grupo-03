package ar.edu.uade.skillswap.application.model;

import java.time.LocalDateTime;

public final class NotificationData {
    private NotificationData() {}

    public record NotificationView(String id, String title, String detail,
                                   LocalDateTime createdAt, boolean read, String type) {}
}
