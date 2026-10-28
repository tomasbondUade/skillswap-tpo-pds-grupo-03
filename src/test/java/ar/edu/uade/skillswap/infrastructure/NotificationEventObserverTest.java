package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationEventObserverTest {
    @Test
    void notifiesOnlyTheOtherBookingParticipantAndAllowsMultipleObservers() {
        var repository = new InMemoryNotificationRepository();
        var publisher = new InMemoryDomainEventPublisher();
        var observer = new NotificationEventObserver(new InAppNotificationGateway(repository));
        var secondObserverCalls = new AtomicInteger();
        publisher.subscribe("booking.requested", observer::onEvent);
        publisher.subscribe("booking.requested", event -> secondObserverCalls.incrementAndGet());

        publisher.publish(new DomainEvent("booking.requested", "Booking", "booking-1", "buyer",
                LocalDateTime.now(), Map.of(
                        "buyerUserId", "buyer",
                        "providerUserId", "provider",
                        "detail", "Hay una nueva solicitud.")));

        assertEquals(0, repository.findByRecipientUserId("buyer").size());
        assertEquals(1, repository.findByRecipientUserId("provider").size());
        assertEquals(1, secondObserverCalls.get());
    }

    @Test
    void reportsObserverFailuresAfterNotifyingRemainingObservers() {
        var publisher = new InMemoryDomainEventPublisher();
        var laterObserverCalls = new AtomicInteger();
        publisher.subscribe("booking.paid", event -> { throw new IllegalStateException("gateway unavailable"); });
        publisher.subscribe("booking.paid", event -> laterObserverCalls.incrementAndGet());
        var event = new DomainEvent("booking.paid", "Booking", "booking-2", "buyer",
                LocalDateTime.now(), Map.of());

        assertThrows(IllegalStateException.class, () -> publisher.publish(event));
        assertEquals(1, laterObserverCalls.get());
    }
}