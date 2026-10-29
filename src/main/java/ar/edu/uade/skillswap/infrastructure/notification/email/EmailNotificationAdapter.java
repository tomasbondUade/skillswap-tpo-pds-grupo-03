package ar.edu.uade.skillswap.infrastructure.notification.email;

import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;

/**
 * Adapta el contrato de Application al cliente externo de email.
 */
public class EmailNotificationAdapter implements NotificationGateway {

    private final MockEmailClient emailClient;

    public EmailNotificationAdapter(MockEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Override
    public void send(NotificationMessage notification) {
        if (notification.channel() != NotificationChannelType.EMAIL) {
            throw new IllegalArgumentException(
                    "Este adapter solo admite notificaciones por email"
            );
        }

        emailClient.sendEmail(
                notification.recipientUserId(),
                notification.title(),
                notification.detail()
        );
    }
}
