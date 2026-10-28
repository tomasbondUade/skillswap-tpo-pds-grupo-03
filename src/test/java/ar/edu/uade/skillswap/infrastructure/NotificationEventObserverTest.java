package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.infrastructure.notification.NotificationFactory;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationEventObserverTest {
    @Test
    void notifiesOnlyTheOtherBookingParticipantAndAllowsMultipleObservers() {
        var repository = new InMemoryNotificationRepository();
        var publisher = new InMemoryDomainEventPublisher();
        var profiles = new InMemoryProfileRepository();
        profiles.save(Profile.register("provider", "Ana", "", "UTC", List.of(), List.of(),
                Set.of(NotificationChannelType.IN_APP), List.of()));
        var factory = new NotificationFactory(repository, new MockEmailClient(), new MockPushClient());
        var observer = new NotificationEventObserver(profiles, factory);
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