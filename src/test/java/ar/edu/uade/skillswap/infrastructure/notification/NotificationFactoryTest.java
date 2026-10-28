package ar.edu.uade.skillswap.infrastructure.notification;

import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.port.out.NotificationGateway;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.infrastructure.notification.email.EmailNotificationAdapter;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.inapp.InAppNotificationAdapter;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.PushNotificationAdapter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationFactoryTest {

    @Test
    void createsEmailAdapter() {
        NotificationGateway gateway =
                createFactory().create(NotificationChannelType.EMAIL);

        assertInstanceOf(EmailNotificationAdapter.class, gateway);
    }

    @Test
    void createsPushAdapter() {
        NotificationGateway gateway =
                createFactory().create(NotificationChannelType.PUSH);

        assertInstanceOf(PushNotificationAdapter.class, gateway);
    }

    @Test
    void createsInAppAdapter() {
        NotificationGateway gateway =
                createFactory().create(NotificationChannelType.IN_APP);

        assertInstanceOf(InAppNotificationAdapter.class, gateway);
    }

    @Test
    void rejectsNullChannel() {
        assertThrows(
                NullPointerException.class,
                () -> createFactory().create(null));
    }

    private NotificationFactory createFactory() {
        return new NotificationFactory(
                new InMemoryNotificationRepository(),
                new MockEmailClient(),
                new MockPushClient());
    }

    private static class InMemoryNotificationRepository
            implements NotificationRepository {

        @Override
        public List<NotificationView> findByRecipientUserId(String userId) {
            return List.of();
        }

        @Override
        public Optional<NotificationView> findByIdAndRecipientUserId(
                String notificationId, String userId) {
            return Optional.empty();
        }

        @Override
        public void save(
                ar.edu.uade.skillswap.application.model.NotificationMessage notification) {
            // Simula el guardado sin utilizar una base de datos.
        }

        @Override
        public void markRead(String notificationId, String userId) {
            // No se necesita para probar la Factory.
        }

        @Override
        public void markAllRead(String userId) {
            // No se necesita para probar la Factory.
        }
    }
}
