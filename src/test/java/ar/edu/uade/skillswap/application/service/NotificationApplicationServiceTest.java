package ar.edu.uade.skillswap.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NotificationApplicationServiceTest {
    @Test
    void scopesInboxAndReadUpdatesToAuthenticatedRecipient() {
        var repository = new InMemoryNotifications();
        repository.items.add(new NotificationView("n-1", "Reserva", "Confirmada", LocalDateTime.now(), false, "BOOKING"));
        var transactions = new ImmediateTransactionManager();
        var service = new NotificationApplicationService(repository, () -> "user-1", transactions);

        assertEquals(1, service.listMine().size());
        service.markRead("n-1");
        assertTrue(repository.items.get(0).read());
        service.markAllRead();
        assertEquals(2, transactions.operations);
    }

    @Test
    void doesNotRevealNotificationsOwnedByAnotherUser() {
        var repository = new InMemoryNotifications();
        repository.items.add(new NotificationView("n-1", "Reserva", "Confirmada", LocalDateTime.now(), false, "BOOKING"));
        var service = new NotificationApplicationService(repository, () -> "user-2", new ImmediateTransactionManager());

        var error = assertThrows(ApplicationException.class, () -> service.markRead("n-1"));

        assertEquals(ApplicationErrorCode.NOT_FOUND, error.code());
        assertFalse(repository.items.get(0).read());
    }

    private static final class InMemoryNotifications implements NotificationRepository {
        private final List<NotificationView> items = new ArrayList<>();

        @Override public List<NotificationView> findByRecipientUserId(String userId) {
            return "user-1".equals(userId) ? List.copyOf(items) : List.of();
        }
        @Override public Optional<NotificationView> findByIdAndRecipientUserId(String id, String userId) {
            return "user-1".equals(userId)
                    ? items.stream().filter(item -> item.id().equals(id)).findFirst()
                    : Optional.empty();
        }
        @Override public void markRead(String id, String userId) {
            for (int i = 0; i < items.size(); i++) {
                NotificationView item = items.get(i);
                if (item.id().equals(id)) {
                    items.set(i, new NotificationView(item.id(), item.title(), item.detail(), item.createdAt(), true, item.type()));
                }
            }
        }
        @Override public void markAllRead(String userId) {
            for (NotificationView item : List.copyOf(items)) markRead(item.id(), userId);
        }
    }
}
