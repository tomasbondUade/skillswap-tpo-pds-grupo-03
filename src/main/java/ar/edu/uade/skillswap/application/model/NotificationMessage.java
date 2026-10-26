package ar.edu.uade.skillswap.application.model;

import ar.edu.uade.skillswap.domain.model.NotificationChannelType;

/** Outbound notification request; concrete email, push or in-app clients stay in Infrastructure. */
public record NotificationMessage(String recipientUserId, String title, String detail,
                                  String type, NotificationChannelType channel) {
    public NotificationMessage {
        if (recipientUserId == null || recipientUserId.isBlank() || title == null || title.isBlank()
                || detail == null || detail.isBlank() || type == null || type.isBlank() || channel == null) {
            throw new IllegalArgumentException("La notificación está incompleta");
        }
        recipientUserId = recipientUserId.trim();
        title = title.trim();
        detail = detail.trim();
        type = type.trim();
    }
}
