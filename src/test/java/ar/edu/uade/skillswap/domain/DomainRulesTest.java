package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.domain.model.RecentPopularityStrategy;
import ar.edu.uade.skillswap.domain.model.SkillAffinityStrategy;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.domain.policy.FlexibleRefundPolicy;
import ar.edu.uade.skillswap.domain.policy.StandardRefundPolicy;
import ar.edu.uade.skillswap.domain.state.BookingStates;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainRulesTest {
    @Test
    void publicationValidatesAvailabilityOwnershipAndStateTransitions() {
        Publication publication = draft("provider", "Java", List.of("Java"));
        assertThrows(IllegalStateException.class, () -> publication.publish("provider"));
        assertThrows(SecurityException.class, () -> publication.addAvailability("other", slot(1)));

        publication.addAvailability("provider", slot(1));
        publication.publish("provider");
        publication.reserve(publication.availability().get(0).id());

        assertEquals(PublicationStatus.PUBLISHED, publication.status());
        assertEquals(0, publication.availability().get(0).remaining());
        assertThrows(IllegalStateException.class, () -> publication.edit(
                "provider", "Otro título", "Descripción", "Tecnología", Modality.REMOTE,
                60, 1, Set.of("Java")));
        assertThrows(UnsupportedOperationException.class, () -> publication.history().clear());
        assertEquals("PUBLISHED", publication.history().get(publication.history().size() - 1).to());
    }

    @Test
    void availabilityRejectsInvalidWindowsAndCapacity() {
        LocalDateTime start = LocalDateTime.parse("2026-11-01T10:00:00");
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.of(start, start, 1));
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.of(start, start.plusHours(1), 0));

        Publication publication = draft("provider", "Java", List.of("Java"));
        assertThrows(IllegalArgumentException.class, () -> publication.addAvailability(
                "provider", AvailabilitySlot.of(start, start.plusHours(1), 2)));
        publication.addAvailability("provider", AvailabilitySlot.of(start, start.plusHours(1), 1));
        assertThrows(IllegalArgumentException.class, () -> publication.addAvailability(
                "provider", AvailabilitySlot.of(start.plusMinutes(30), start.plusHours(2), 1)));
    }

    @Test
    void availabilityReservationsCannotExceedCapacityOrReleaseAnEmptySlot() {
        Publication publication = draft("provider", "Java", List.of("Java"));
        publication.addAvailability("provider", slot(1));
        publication.publish("provider");
        String slotId = publication.availability().get(0).id();

        publication.reserve(slotId);
        assertEquals(0, publication.availability().get(0).remaining());
        assertThrows(IllegalStateException.class, () -> publication.reserve(slotId));
        assertEquals(1, publication.availability().get(0).reserved());

        publication.releaseReservation(slotId);
        assertEquals(1, publication.availability().get(0).remaining());
        assertThrows(IllegalStateException.class, () -> publication.releaseReservation(slotId));
        assertEquals(0, publication.availability().get(0).reserved());
    }

    @Test
    void bookingRequestMustTargetPublishedOfferOwnedBySomeoneElseAndAnAvailableSlot() {
        Publication publication = draft("provider", "Java", List.of("Java"));
        publication.addAvailability("provider", slot(1));
        String slotId = publication.availability().get(0).id();
        assertThrows(IllegalStateException.class, () -> Booking.request(
                publication, "buyer", "Objetivo", slotId, new SkillCoins(20)));

        publication.publish("provider");
        assertThrows(IllegalArgumentException.class, () -> Booking.request(
                publication, "provider", "Objetivo", slotId, new SkillCoins(20)));
        Booking.request(publication, "buyer", "Objetivo", slotId, new SkillCoins(20));
        assertEquals(1, publication.requestCount());

        assertThrows(IllegalArgumentException.class, () -> Booking.request(
                publication, "another-buyer", "Objetivo", "missing-slot", new SkillCoins(20)));

        publication.reserve(slotId);
        assertThrows(IllegalStateException.class, () -> Booking.request(
                publication, "another-buyer", "Objetivo", slotId, new SkillCoins(20)));
    }

    @Test
    void bookingRequestKeepsOptionalClientMessageAndRequiredObjective() {
        Publication publication = draft("provider", "Java", List.of("Java"));
        publication.addAvailability("provider", slot(1));
        publication.publish("provider");
        String slotId = publication.availability().get(0).id();

        Booking booking = Booking.request(publication, "buyer", "  Necesito prepararme para una entrevista  ",
                "  Practicar diseño de APIs  ", slotId, new SkillCoins(20));

        assertEquals("Necesito prepararme para una entrevista", booking.clientMessage());
        assertEquals("Practicar diseño de APIs", booking.objective());
        assertThrows(IllegalArgumentException.class, () -> Booking.request(
                publication, "another-buyer", null, " ", slotId, new SkillCoins(20)));
    }

    @Test
    void bookingEnforcesParticipantsTransitionsAndImmutableHistory() {
        Booking booking = booking(120);
        assertThrows(SecurityException.class, () -> booking.accept("buyer"));

        booking.accept("provider");
        booking.confirmPayment("buyer");
        booking.start("provider");
        booking.complete("buyer");

        assertEquals(BookingStatus.COMPLETED, booking.status());
        assertEquals(5, booking.history().size());
        assertThrows(UnsupportedOperationException.class, () -> booking.history().clear());
        assertThrows(IllegalStateException.class, () -> booking.complete("buyer"));
    }

    @Test
    void bookingStateSingletonsAreStatelessAndDoNotShareBookingData() {
        assertSame(BookingStates.pending(), BookingStates.pending());

        Booking first = booking(100);
        Booking second = booking(200);
        first.accept("provider");

        assertEquals(BookingStatus.ACCEPTED, first.status());
        assertEquals(BookingStatus.PENDING, second.status());
        assertEquals(2, first.history().size());
        assertEquals(1, second.history().size());
        assertThrows(UnsupportedOperationException.class, () -> second.history().clear());
    }

    @Test
    void bookingRejectsRefundAbovePriceWithoutChangingState() {
        Booking booking = booking(100);
        booking.accept("provider");
        booking.confirmPayment("buyer");

        assertThrows(IllegalArgumentException.class,
                () -> booking.cancel("buyer", "Cambio de planes", new SkillCoins(101)));
        assertEquals(BookingStatus.CONFIRMED, booking.status());
        booking.cancel("buyer", "Cambio de planes", new SkillCoins(50));
        assertEquals(50, booking.refund().amount());
    }

    @Test
    void walletKeepsBalanceAndLedgerConsistent() {
        Wallet wallet = Wallet.open("buyer", 50);
        assertThrows(IllegalArgumentException.class, () -> Wallet.open("invalid", -1));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(51, "pago"));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(1, " "));
        assertEquals(50, wallet.balance().amount());
        assertEquals(1, wallet.movements().size());

        wallet.debit(20, "Pago de mentoría");
        wallet.credit(10, "Reintegro");
        assertEquals(40, wallet.balance().amount());
        assertEquals(3, wallet.movements().size());
        assertEquals(40, wallet.movements().get(wallet.movements().size() - 1).resultingBalance());
        assertThrows(UnsupportedOperationException.class, () -> wallet.movements().clear());
    }

    @Test
    void profilesProtectOwnershipAvailabilityAndDerivedReputation() {
        Profile profile = Profile.register("provider", "Mentora", "", "America/Argentina/Buenos_Aires",
                Set.of("Java"), Set.of("Backend"), List.of(NotificationChannelType.IN_APP));
        LocalDateTime start = LocalDateTime.parse("2026-11-01T10:00:00");
        assertThrows(SecurityException.class,
                () -> profile.updateAvailability("other", List.of(slot(1))));
        assertThrows(IllegalArgumentException.class, () -> profile.updateAvailability("provider", List.of(
                AvailabilitySlot.of(start, start.plusHours(2), 1),
                AvailabilitySlot.of(start.plusHours(1), start.plusHours(3), 1))));

        profile.deactivate("provider");
        assertFalse(profile.active());
        assertThrows(IllegalStateException.class, () -> profile.updateDetails(
                "provider", "Nombre", "bio", "UTC", Set.of(), Set.of()));
    }

    @Test
    void profileActivatesAndDeactivatesLogicallyOnlyForItsOwner() {
        Profile profile = Profile.register("provider", "Mentora", "", "UTC",
                Set.of("Java"), Set.of("Backend"), List.of());
        Booking historicalBooking = booking(120);
        historicalBooking.accept("provider");
        List<?> bookingHistoryBeforeDeactivation = historicalBooking.history();

        assertThrows(IllegalStateException.class, () -> profile.activate("provider"));
        assertThrows(SecurityException.class, () -> profile.deactivate("other"));

        profile.deactivate("provider");
        assertFalse(profile.active());
        assertEquals(BookingStatus.ACCEPTED, historicalBooking.status());
        assertEquals(bookingHistoryBeforeDeactivation, historicalBooking.history());
        assertThrows(IllegalStateException.class, () -> profile.deactivate("provider"));
        assertThrows(SecurityException.class, () -> profile.activate("other"));

        profile.activate("provider");
        assertTrue(profile.active());
        assertThrows(IllegalStateException.class, () -> profile.activate("provider"));
    }

    @Test
    void profileRejectsInvalidIdentityAndDoesNotPartiallyApplyInvalidDetails() {
        assertThrows(IllegalArgumentException.class, () -> Profile.register(" ", "Mentora", "", "UTC",
                Set.of(), Set.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> Profile.register("provider", "Mentora", "", "UTC",
                Set.of(" "), Set.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> Profile.register("provider", "Mentora", "", " ",
                Set.of(), Set.of(), List.of()));

        Profile profile = Profile.register("provider", "Mentora", "Bio", "UTC",
                List.of(" Java ", "Java", "SQL"), List.of("Backend"), List.of());
        assertEquals(Set.of("Java", "SQL"), profile.skills());
        assertThrows(IllegalArgumentException.class, () -> profile.updateDetails(
                "provider", "Nuevo nombre", "Nueva bio", "UTC-3", List.of("Java", " "), List.of("Backend")));
        assertEquals("Mentora", profile.displayName());
        assertEquals("Bio", profile.biography());
        assertEquals("UTC", profile.timeZone());
        assertEquals(Set.of("Java", "SQL"), profile.skills());
    }

    @Test
    void ratingCanOnlyBeCreatedByParticipantAfterCompletedBooking() {
        Booking booking = booking(10);
        assertThrows(IllegalStateException.class,
                () -> Rating.forCompletedBooking(booking, "buyer", 5, "Excelente"));
        booking.accept("provider");
        booking.confirmPayment("buyer");
        booking.start("provider");
        booking.complete("buyer");

        assertThrows(SecurityException.class,
                () -> Rating.forCompletedBooking(booking, "outsider", 5, "Excelente"));
        assertThrows(IllegalArgumentException.class,
                () -> Rating.forCompletedBooking(booking, "buyer", 6, "Excelente"));
        Rating rating = Rating.forCompletedBooking(booking, "buyer", 5, "  Excelente  ");
        assertEquals("provider", rating.subjectUserId());
        assertEquals("Excelente", rating.comment());
    }

    @Test
    void profileRecalculatesReputationAndRejectsDuplicateRatings() {
        Profile profile = Profile.register("provider", "Mentora", "", "UTC",
                Set.of("Java"), Set.of("Backend"), List.of());
        Rating first = Rating.restore("rating-1", "booking-1", "buyer-1", "provider",
                5, "Muy clara", LocalDateTime.parse("2026-10-01T10:00:00"));
        Rating second = Rating.restore("rating-2", "booking-2", "buyer-2", "provider",
                4, "Útil", LocalDateTime.parse("2026-10-02T10:00:00"));

        profile.recalculateReputation(List.of(first, second));

        assertEquals(4.5, profile.reputation());
        assertEquals(2, profile.ratingCount());

        Rating duplicate = Rating.restore("rating-3", "booking-1", "buyer-1", "provider",
                1, "Duplicada", LocalDateTime.parse("2026-10-03T10:00:00"));
        assertThrows(IllegalArgumentException.class,
                () -> profile.recalculateReputation(List.of(first, duplicate)));
        assertEquals(4.5, profile.reputation(), "Un cálculo inválido no debe modificar la reputación");

        profile.recalculateReputation(List.of());
        assertEquals(0.0, profile.reputation());
        assertEquals(0, profile.ratingCount());
        assertThrows(IllegalArgumentException.class, () -> profile.recalculateReputation(List.of(
                Rating.restore("rating-other", "booking-3", "buyer-3", "other-user",
                        5, "Otra persona", LocalDateTime.now()))));
    }

    @Test
    void refundStrategiesOfferDifferentRulesForTheSameBooking() {
        Booking booking = booking(200);
        LocalDateTime cancellation = LocalDateTime.parse("2026-10-01T10:00:00");

        assertEquals(100, new StandardRefundPolicy().refundFor(
                booking, cancellation, cancellation.plusHours(36)).amount());
        assertEquals(200, new FlexibleRefundPolicy().refundFor(
                booking, cancellation, cancellation.plusHours(36)).amount());
        assertEquals(0, new StandardRefundPolicy().refundFor(
                booking, cancellation, cancellation.plusHours(12)).amount());
    }

    @Test
    void recommendationStrategiesRankCandidatesByDifferentBusinessRules() {
        Profile profile = Profile.register("buyer", "Buyer", "", "America/Argentina/Buenos_Aires",
                Set.of("Java"), Set.of("Backend"), List.of());
        Publication affinity = published("provider-a", "Java para entrevistas", List.of("Java"), 1);
        Publication popular = published("provider-b", "Comunicación", List.of("Liderazgo"), 3);

        assertEquals(affinity.id(), new SkillAffinityStrategy()
                .recommend(profile, List.of(popular, affinity), 2).get(0).id());
        assertEquals(popular.id(), new RecentPopularityStrategy()
                .recommend(profile, List.of(affinity, popular), 2).get(0).id());
        assertThrows(IllegalArgumentException.class,
                () -> new SkillAffinityStrategy().recommend(profile, List.of(), -1));
    }

    @Test
    void skillCoinsRejectNegativeBalancesAndCheckOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(1).minus(new SkillCoins(2)));
        assertThrows(ArithmeticException.class, () -> new SkillCoins(Integer.MAX_VALUE).plus(new SkillCoins(1)));
        assertEquals(0, new SkillCoins(2).minus(new SkillCoins(2)).amount());
    }

    private static Publication draft(String owner, String title, List<String> tags) {
        return Publication.draft(owner, title, "Descripción", "Tecnología", Modality.REMOTE,
                60, 1, new SkillCoins(100), tags);
    }

    private static Publication published(String owner, String title, List<String> tags, int requests) {
        Publication publication = Publication.draft(owner, title, "Descripción", "Tecnología",
                Modality.REMOTE, 60, 2, new SkillCoins(100), tags);
        LocalDateTime startsAt = LocalDateTime.now().plusDays(7);
        publication.addAvailability(owner, AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2));
        publication.publish(owner);
        for (int index = 0; index < requests; index++) {
            Booking.request(publication, "buyer-" + index, "Objetivo", publication.availability().get(0).id(),
                    publication.basePrice());
        }
        return publication;
    }

    private static Booking booking(int amount) {
        Publication publication = published("provider", "Java", List.of("Java"), 0);
        return Booking.request(publication, "buyer", "Objetivo", publication.availability().get(0).id(),
                new SkillCoins(amount));
    }

    private static AvailabilitySlot slot(int capacity) {
        LocalDateTime start = LocalDateTime.parse("2026-11-01T10:00:00");
        return AvailabilitySlot.of(start, start.plusHours(1), capacity);
    }
}
