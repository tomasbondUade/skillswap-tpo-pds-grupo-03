package ar.edu.uade.skillswap.infrastructure.notification;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.model.NotificationData.NotificationView;
import ar.edu.uade.skillswap.application.model.NotificationMessage;
import ar.edu.uade.skillswap.application.port.out.NotificationRepository;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingNotificationEventPublisherTest {

    @Test
    void sendsNotificationsThroughEveryConfiguredChannel() {
        Profile recipient = profile("provider");
        recipient.setNotificationChannels(
                "provider",
                Set.of(NotificationChannelType.EMAIL, NotificationChannelType.PUSH));

        ProfileRepository profiles = new InMemoryProfileRepository(recipient);
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();

        List<String> emailCalls = new ArrayList<>();
        List<String> pushCalls = new ArrayList<>();

        MockEmailClient emailClient = new MockEmailClient() {
            @Override
            public void sendEmail(String recipient, String subject, String body) {
                emailCalls.add(recipient + "|" + subject + "|" + body);
            }
        };

        MockPushClient pushClient = new MockPushClient() {
            @Override
            public void sendPush(String recipient, String title, String message) {
                pushCalls.add(recipient + "|" + title + "|" + message);
            }
        };

        NotificationFactory factory = new NotificationFactory(
                notifications, emailClient, pushClient);

        RatingNotificationEventPublisher publisher =
                new RatingNotificationEventPublisher(profiles, factory);

        publisher.publish(ratingEvent("provider", "5", "Excelente"));

        assertEquals(1, emailCalls.size());
        assertEquals(1, pushCalls.size());
        assertEquals(0, notifications.saved.size());

        assertEquals(true, emailCalls.get(0).contains("provider"));
        assertEquals(true, emailCalls.get(0).contains("5/5"));
        assertEquals(true, pushCalls.get(0).contains("provider"));
        assertEquals(true, pushCalls.get(0).contains("Excelente"));
    }

    @Test
    void savesInAppNotificationWhenRecipientPrefersInApp() {
        Profile recipient = profile("provider");
        recipient.setNotificationChannels(
                "provider", Set.of(NotificationChannelType.IN_APP));

        InMemoryNotificationRepository notifications =
                new InMemoryNotificationRepository();

        NotificationFactory factory = new NotificationFactory(
                notifications, new MockEmailClient(), new MockPushClient());

        RatingNotificationEventPublisher publisher =
                new RatingNotificationEventPublisher(
                        new InMemoryProfileRepository(recipient), factory);

        publisher.publish(ratingEvent("provider", "4", "Muy buena experiencia"));

        assertEquals(1, notifications.saved.size());

        NotificationMessage saved = notifications.saved.get(0);
        assertEquals("provider", saved.recipientUserId());
        assertEquals("RATING_RECEIVED", saved.type());
        assertEquals(NotificationChannelType.IN_APP, saved.channel());
        assertEquals(true, saved.detail().contains("Muy buena experiencia"));
    }

    @Test
    void ignoresEventsThatAreNotRatingSubmitted() {
        Profile recipient = profile("provider");
        InMemoryNotificationRepository notifications =
                new InMemoryNotificationRepository();

        NotificationFactory factory = new NotificationFactory(
                notifications, new MockEmailClient(), new MockPushClient());

        RatingNotificationEventPublisher publisher =
                new RatingNotificationEventPublisher(
                        new InMemoryProfileRepository(recipient), factory);

        DomainEvent event = new DomainEvent(
                "BookingCreated",
                "Booking",
                "booking-1",
                "buyer",
                LocalDateTime.now(),
                Map.of());

        publisher.publish(event);

        assertEquals(0, notifications.saved.size());
    }

    @Test
    void rejectsRatingEventWithoutRecipient() {
        RatingNotificationEventPublisher publisher =
                new RatingNotificationEventPublisher(
                        new InMemoryProfileRepository(profile("provider")),
                        new NotificationFactory(
                                new InMemoryNotificationRepository(),
                                new MockEmailClient(),
                                new MockPushClient()));

        DomainEvent event = new DomainEvent(
                "RatingSubmitted",
                "Rating",
                "rating-1",
                "buyer",
                LocalDateTime.now(),
                Map.of("score", "5"));

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(event));
    }

    private static Profile profile(String userId) {
        return Profile.register(
                userId, userId, "", "UTC",
                List.of(), List.of(), List.of());
    }

    private static DomainEvent ratingEvent(
            String recipient, String score, String comment) {

        return new DomainEvent(
                "RatingSubmitted",
                "Rating",
                "rating-1",
                "buyer",
                LocalDateTime.now(),
                Map.of(
                        "recipientUserId", recipient,
                        "score", score,
                        "comment", comment));
    }

    private static class InMemoryProfileRepository
            implements ProfileRepository {

        private final Map<String, Profile> profiles = new HashMap<>();

        InMemoryProfileRepository(Profile... initialProfiles) {
            for (Profile profile : initialProfiles) {
                profiles.put(profile.userId(), profile);
            }
        }

        @Override
        public Optional<Profile> findByUserId(String userId) {
            return Optional.ofNullable(profiles.get(userId));
        }

        @Override
        public Optional<Profile> findByUserIdForUpdate(String userId) {
            return findByUserId(userId);
        }

        @Override
        public Profile save(Profile profile) {
            profiles.put(profile.userId(), profile);
            return profile;
        }
    }

    private static class InMemoryNotificationRepository
            implements NotificationRepository {

        private final List<NotificationMessage> saved = new ArrayList<>();

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
        public void save(NotificationMessage notification) {
            saved.add(notification);
        }

        @Override
        public void markRead(String notificationId, String userId) {
        }

        @Override
        public void markAllRead(String userId) {
        }
    }
}
