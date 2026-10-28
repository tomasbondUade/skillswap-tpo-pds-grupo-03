package ar.edu.uade.skillswap.infrastructure.notification.inapp;

import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;

import java.util.Objects;

/**
 * Adapta las solicitudes de notificación al guardado
 * en la bandeja interna de SkillSwap.
 */
public class InAppNotificationAdapter implements NotificationGateway {

    private final NotificationRepository notificationRepository;

    public InAppNotificationAdapter(
            NotificationRepository notificationRepository) {
        this.notificationRepository =
                Objects.requireNonNull(notificationRepository);
    }

    @Override
    public void send(NotificationMessage notification) {
        Objects.requireNonNull(notification, "Falta la notificación");

        if (notification.channel() != NotificationChannelType.IN_APP) {
            throw new IllegalArgumentException(
                    "Este adapter solo admite notificaciones IN_APP");
        }

        notificationRepository.save(notification);
    }
}
