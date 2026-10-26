package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import java.util.List;

/** Inbound port for the authenticated user's in-app notification inbox. */
public interface NotificationUseCases {
    List<NotificationView> listMine();
    void markRead(String notificationId);
    void markAllRead();
}
