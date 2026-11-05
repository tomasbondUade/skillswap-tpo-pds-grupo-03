package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import java.util.List;
import java.util.Optional;

/** Contrato de persistencia para las notificaciones de la aplicación. */
public interface NotificationRepository {
    List<NotificationView> findByRecipientUserId(String userId);

    Optional<NotificationView> findByIdAndRecipientUserId(
            String notificationId, String userId);

    void save(NotificationMessage notification);

    void markRead(String notificationId, String userId);

    void markAllRead(String userId);
}
