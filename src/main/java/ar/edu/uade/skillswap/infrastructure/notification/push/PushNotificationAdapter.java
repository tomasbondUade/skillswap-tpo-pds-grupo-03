package ar.edu.uade.skillswap.infrastructure.notification.push;

import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;

/**
 * Adapta el contrato de Application al cliente externo de push.
 */
public class PushNotificationAdapter implements NotificationGateway {

    private final MockPushClient pushClient;

    public PushNotificationAdapter(MockPushClient pushClient) {
        this.pushClient = pushClient;
    }

    @Override
    public void send(NotificationMessage notification) {
        if (notification.channel() != NotificationChannelType.PUSH) {
            throw new IllegalArgumentException(
                    "Este adapter solo admite notificaciones push"
            );
        }

        pushClient.sendPush(
                notification.recipientUserId(),
                notification.title(),
                notification.detail()
        );
    }
}
