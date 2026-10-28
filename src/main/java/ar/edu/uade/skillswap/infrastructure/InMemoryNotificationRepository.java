package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryNotificationRepository implements NotificationRepository {
    private record StoredNotification(String recipientUserId, NotificationView view) {}

    private final Map<String, StoredNotification> notifications = new LinkedHashMap<>();

    @Override
    public List<NotificationView> findByRecipientUserId(String userId) {
        return notifications.values().stream()
                .filter(notification -> notification.recipientUserId().equals(userId))
                .map(StoredNotification::view)
                .toList();
    }

    @Override
    public Optional<NotificationView> findByIdAndRecipientUserId(String notificationId, String userId) {
        return Optional.ofNullable(notifications.get(notificationId))
                .filter(notification -> notification.recipientUserId().equals(userId))
                .map(StoredNotification::view);
    }

    @Override
    public void markRead(String notificationId, String userId) {
        findByIdAndRecipientUserId(notificationId, userId).ifPresent(notification ->
                notifications.put(notificationId, new StoredNotification(userId, new NotificationView(
                        notification.id(), notification.title(), notification.detail(),
                notification.createdAt(), true, notification.type()))));
    }

    @Override
    public void markAllRead(String userId) {
        findByRecipientUserId(userId).forEach(notification -> markRead(notification.id(), userId));
    }

    public void save(NotificationView notification, String recipientUserId) {
        notifications.put(notification.id(), new StoredNotification(recipientUserId, notification));
    }
}