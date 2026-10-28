package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.Rating;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingApplicationServiceTest {
    @Test
    void submitsRatingAndUpdatesTheSubjectsDerivedReputationInOneTransaction() {
        Fixture fixture = new Fixture();

        var view = fixture.service.submit(new SubmitCommand(fixture.booking.id(), 5, "  Excelente  "));

        assertEquals(fixture.booking.id(), view.bookingId());
        assertEquals("buyer", view.authorUserId());
        assertEquals("provider", view.subjectUserId());
        assertEquals("Excelente", view.comment());
        assertEquals(1, fixture.records.size());
        assertEquals(5.0, fixture.profiles.get("provider").reputation());
        assertEquals(1, fixture.profiles.get("provider").ratingCount());
        assertEquals(1, fixture.transactions.operations);
    }

    @Test
    void rejectsTheSameAuthorRatingTheSameBookingTwice() {
        Fixture fixture = new Fixture();
        fixture.service.submit(new SubmitCommand(fixture.booking.id(), 5, "Primera calificación"));

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> fixture.service.submit(new SubmitCommand(fixture.booking.id(), 4, "Repetida")));

        assertEquals(ApplicationErrorCode.CONFLICT, error.code());
        assertEquals(1, fixture.records.size());
        assertEquals(5.0, fixture.profiles.get("provider").reputation());
    }

    @Test
    void allowsEachParticipantToRateTheOtherOnce() {
        Fixture fixture = new Fixture();
        fixture.service.submit(new SubmitCommand(fixture.booking.id(), 5, "Del cliente"));
        fixture.actor = "provider";

        var providerRating = fixture.service.submit(
                new SubmitCommand(fixture.booking.id(), 4, "Del proveedor"));

        assertEquals("provider", providerRating.authorUserId());
        assertEquals("buyer", providerRating.subjectUserId());
        assertEquals(2, fixture.records.size());
        assertEquals(5.0, fixture.profiles.get("provider").reputation());
        assertEquals(4.0, fixture.profiles.get("buyer").reputation());
    }

    private static final class Fixture {
        private final Booking booking = completedBooking();
        private final Map<String, Profile> profiles = new HashMap<>();
        private final List<Rating> records = new ArrayList<>();
        private final RatingStore ratings = new RatingStore();
        private final ImmediateTransactionManager transactions = new ImmediateTransactionManager();
        private String actor = "buyer";
        private final RatingApplicationService service;

        private Fixture() {
            profiles.put("buyer", profile("buyer"));
            profiles.put("provider", profile("provider"));
            ProfileStore profileStore = new ProfileStore();
            BookingRepository bookingStore = new BookingRepository() {
                @Override public Optional<Booking> findById(String bookingId) {
                    return booking.id().equals(bookingId) ? Optional.of(booking) : Optional.empty();
                }
                @Override public Optional<Booking> findByIdForUpdate(String bookingId) {
                    return findById(bookingId);
                }
                @Override public List<Booking> findByParticipantUserId(String userId) {
                    return List.of(booking);
                }
                @Override public Booking save(Booking value) { return value; }
            };
            service = new RatingApplicationService(bookingStore, ratings, profileStore,
                    () -> actor, transactions);
        }

        private Profile profile(String userId) {
            return Profile.register(userId, userId, "", "UTC", List.of(), List.of(), List.of());
        }

        private final class ProfileStore implements ProfileRepository {
            @Override public Optional<Profile> findByUserId(String userId) {
                return Optional.ofNullable(profiles.get(userId));
            }
            @Override public Optional<Profile> findByUserIdForUpdate(String userId) {
                return findByUserId(userId);
            }
            @Override public Profile save(Profile profile) {
                profiles.put(profile.userId(), profile);
                return profile;
            }
        }

        private final class RatingStore implements RatingRepository {
            @Override public boolean existsByBookingIdAndAuthorUserId(String bookingId, String authorUserId) {
                return records.stream().anyMatch(rating -> rating.bookingId().equals(bookingId)
                        && rating.authorUserId().equals(authorUserId));
            }
            @Override public List<Rating> findBySubjectUserId(String subjectUserId) {
                return records.stream().filter(rating -> rating.subjectUserId().equals(subjectUserId)).toList();
            }
            @Override public Rating save(Rating rating) {
                records.add(rating);
                return rating;
            }
        }
    }

    private static Booking completedBooking() {
        Publication publication = Publication.draft("provider", "Java", "Descripción", "Tecnología",
                Modality.REMOTE, 60, 1, new SkillCoins(20), List.of("Java"));
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        publication.addAvailability("provider", AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 1));
        publication.publish("provider");
        Booking booking = Booking.request(publication, "buyer", "Practicar entrevistas",
                publication.availability().get(0).id(), new SkillCoins(20));
        booking.accept("provider");
        booking.confirmPayment("buyer");
        booking.start("provider");
        booking.complete("buyer");
        return booking;
    }
}
