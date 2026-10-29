package ar.edu.uade.skillswap.infrastructure.notification;

import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.infrastructure.notification.email.EmailNotificationAdapter;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.inapp.InAppNotificationAdapter;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.PushNotificationAdapter;

import java.util.Objects;

/**
 * Simple Factory: crea el adaptador según el canal elegido.
 */
public final class NotificationFactory {

    private final NotificationRepository notificationRepository;
    private final MockEmailClient emailClient;
    private final MockPushClient pushClient;

    public NotificationFactory(
            NotificationRepository notificationRepository,
            MockEmailClient emailClient,
            MockPushClient pushClient) {
        this.notificationRepository =
                Objects.requireNonNull(notificationRepository);
        this.emailClient = Objects.requireNonNull(emailClient);
        this.pushClient = Objects.requireNonNull(pushClient);
    }

    public NotificationGateway create(NotificationChannelType channel) {
        Objects.requireNonNull(channel, "Falta el canal de notificación");

        return switch (channel) {
            case EMAIL -> new EmailNotificationAdapter(emailClient);
            case PUSH -> new PushNotificationAdapter(pushClient);
            case IN_APP -> new InAppNotificationAdapter(notificationRepository);
        };
    }
}
