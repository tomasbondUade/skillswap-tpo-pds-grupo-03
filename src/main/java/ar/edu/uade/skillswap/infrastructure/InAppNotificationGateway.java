package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;

import java.time.LocalDateTime;
import java.util.UUID;

public final class InAppNotificationGateway implements NotificationGateway {
    private final InMemoryNotificationRepository notifications;

    public InAppNotificationGateway(InMemoryNotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public void send(NotificationMessage message) {
        if (message.channel() != NotificationChannelType.IN_APP) {
            throw new UnsupportedOperationException("La demo solo entrega notificaciones dentro de la aplicación");
        }
        notifications.save(new NotificationView(
                UUID.randomUUID().toString(),
                message.title(),
                message.detail(),
                LocalDateTime.now(),
                false,
                message.type()), message.recipientUserId());
    }
}