package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import java.util.List;
import java.util.Optional;

/** Read/acknowledgement contract for the user's in-app notification inbox. */
public interface NotificationRepository {
    List<NotificationView> findByRecipientUserId(String userId);
    Optional<NotificationView> findByIdAndRecipientUserId(String notificationId, String userId);
    void markRead(String notificationId, String userId);
    void markAllRead(String userId);
}
