package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.port.out.BookingRepository;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.DomainEventPublisher;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.WalletRepository;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.Wallet;
import ar.edu.uade.skillswap.domain.policy.RefundPolicy;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingApplicationServiceTest {
    @Test
    void noReservaOtroCupoSiLaContratacionYaFueAceptada() {
        Fixture fixture = new Fixture(2);
        fixture.service.accept(new BookingCommand(fixture.booking.id()));
        assertEquals(1, fixture.publication.availability().get(0).remaining());

        var error = assertThrows(ApplicationException.class,
                () -> fixture.service.accept(new BookingCommand(fixture.booking.id())));

        assertEquals(ApplicationErrorCode.INVALID_STATE, error.code());
        assertEquals(1, fixture.publication.availability().get(0).remaining());
        assertEquals(1, fixture.publishedEvents.size());
    }


    @Test
    void acceptsBookingAndReservesPublicationSlot() {
        Fixture fixture = new Fixture();

        var view = fixture.service.accept(
                new BookingCommand(fixture.booking.id()));

        assertEquals(BookingStatus.ACCEPTED, view.status());
        assertEquals(
                0,
                fixture.publication.availability().get(0).remaining());

        assertEquals(1, fixture.publishedEvents.size());
        assertEquals(
                "booking.accepted",
                fixture.publishedEvents.get(0).name());
        assertEquals(1, fixture.transactions.operations);
    }

    @Test
    void paysBookingAndUpdatesBothWallets() {
        Fixture fixture = new Fixture();
        fixture.acceptBooking();

        fixture.actor = "buyer";

        var view = fixture.service.pay(
                new BookingCommand(fixture.booking.id()));

        assertEquals(BookingStatus.CONFIRMED, view.status());
        assertEquals(30, fixture.wallets.get("buyer").balance().amount());
        assertEquals(30, fixture.wallets.get("provider").balance().amount());

        assertEquals(1, fixture.publishedEvents.size());
        assertEquals("booking.paid", fixture.publishedEvents.get(0).name());
    }

    @Test
    void rejectsPaymentWhenBuyerHasInsufficientBalance() {
        Fixture fixture = new Fixture();
        fixture.acceptBooking();

        fixture.wallets.put("buyer", Wallet.open("buyer", 5));
        fixture.actor = "buyer";

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> fixture.service.pay(
                        new BookingCommand(fixture.booking.id())));

        assertEquals(ApplicationErrorCode.VALIDATION, error.code());
        assertEquals(5, fixture.wallets.get("buyer").balance().amount());
        assertEquals(10, fixture.wallets.get("provider").balance().amount());
        assertEquals(BookingStatus.ACCEPTED, fixture.booking.status());
        assertEquals(0, fixture.publishedEvents.size());
    }

    @Test
    void rejectsPaymentFromSomeoneWhoIsNotTheBuyer() {
        Fixture fixture = new Fixture();
        fixture.acceptBooking();

        fixture.actor = "intruder";

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> fixture.service.pay(
                        new BookingCommand(fixture.booking.id())));

        assertEquals(ApplicationErrorCode.FORBIDDEN, error.code());
        assertEquals(50, fixture.wallets.get("buyer").balance().amount());
        assertEquals(10, fixture.wallets.get("provider").balance().amount());
        assertEquals(BookingStatus.ACCEPTED, fixture.booking.status());
        assertEquals(0, fixture.publishedEvents.size());
    }

    @Test
    void doesNotChargeBookingTwice() {
        Fixture fixture = new Fixture();
        fixture.acceptBooking();
        fixture.actor = "buyer";

        fixture.service.pay(new BookingCommand(fixture.booking.id()));

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> fixture.service.pay(
                        new BookingCommand(fixture.booking.id())));

        assertEquals(ApplicationErrorCode.INVALID_STATE, error.code());
        assertEquals(30, fixture.wallets.get("buyer").balance().amount());
        assertEquals(30, fixture.wallets.get("provider").balance().amount());
        assertEquals(1, fixture.publishedEvents.size());
    }

    private static final class Fixture {

        private final List<DomainEvent> publishedEvents = new ArrayList<>();
        private final Map<String, Booking> bookings = new HashMap<>();
        private final Map<String, Publication> publications = new HashMap<>();
        private final Map<String, Wallet> wallets = new HashMap<>();

        private final ImmediateTransactionManager transactions =
                new ImmediateTransactionManager();

        private String actor = "provider";

        private final Publication publication;
        private final Booking booking;
        private final BookingApplicationService service;

        private Fixture() {
            this(1);
        }

        private Fixture(int capacity) {
            publication = Publication.draft(
                    "provider",
                    "Java",
                    "Clases de Java",
                    "Tecnologia",
                    Modality.REMOTE,
                    60,
                    capacity,
                    new SkillCoins(20),
                    List.of("Java"));

            LocalDateTime startsAt = LocalDateTime.now().plusDays(3);

            publication.addAvailability(
                    "provider",
                    AvailabilitySlot.of(
                            startsAt,
                            startsAt.plusHours(1),
                            capacity));

            publication.publish("provider");

            String slotId = publication.availability().get(0).id();

            booking = Booking.request(
                    publication,
                    "buyer",
                    "Aprender Java",
                    slotId,
                    publication.basePrice());

            bookings.put(booking.id(), booking);
            publications.put(publication.id(), publication);

            wallets.put("buyer", Wallet.open("buyer", 50));
            wallets.put("provider", Wallet.open("provider", 10));

            BookingRepository bookingRepository = new BookingRepository() {
                @Override
                public Optional<Booking> findById(String id) {
                    return Optional.ofNullable(bookings.get(id));
                }

                @Override
                public Optional<Booking> findByIdForUpdate(String id) {
                    return findById(id);
                }

                @Override
                public List<Booking> findByParticipantUserId(String userId) {
                    return bookings.values().stream()
                            .filter(b -> b.buyerUserId().equals(userId)
                                    || b.providerUserId().equals(userId))
                            .toList();
                }

                @Override
                public Booking save(Booking value) {
                    bookings.put(value.id(), value);
                    return value;
                }
            };

            PublicationRepository publicationRepository =
                    new PublicationRepository() {
                @Override
                public Optional<Publication> findById(String id) {
                    return Optional.ofNullable(publications.get(id));
                }

                @Override
                public Optional<Publication> findByIdForUpdate(String id) {
                    return findById(id);
                }

                @Override
                public List<Publication> findByAuthorUserId(String authorId) {
                    return publications.values().stream()
                            .filter(p -> p.authorUserId().equals(authorId))
                            .toList();
                }

                @Override
                public List<Publication> findPublished() {
                    return publications.values().stream()
                            .filter(p -> p.status().name().equals("PUBLISHED"))
                            .toList();
                }

                @Override
                public PageResult<Publication> searchPublished(SearchQuery query) {
                    List<Publication> items = findPublished();
                    return new PageResult<>(
                            items.stream().limit(query.size()).toList(),
                            query.page(),
                            query.size(),
                            items.size());
                }

                @Override
                public Publication save(Publication value) {
                    publications.put(value.id(), value);
                    return value;
                }
            };

            WalletRepository walletRepository = new WalletRepository() {
                @Override
                public Optional<Wallet> findByUserId(String userId) {
                    return Optional.ofNullable(wallets.get(userId));
                }

                @Override
                public Optional<Wallet> findByUserIdForUpdate(String userId) {
                    return findByUserId(userId);
                }

                @Override
                public Wallet save(Wallet wallet) {
                    wallets.put(wallet.userId(), wallet);
                    return wallet;
                }
            };

            CurrentUserProvider currentUser = () -> actor;
            DomainEventPublisher eventPublisher = publishedEvents::add;

            // Estos tests no ejercitan cancelación; la política no se invoca.
            RefundPolicy refundPolicy =
                    (ignoredBooking, cancelledAt, sessionStartsAt) ->
                            SkillCoins.zero();

            service = new BookingApplicationService(
                    bookingRepository,
                    publicationRepository,
                    walletRepository,
                    currentUser,
                    transactions,
                    refundPolicy,
                    eventPublisher);
        }

        private void acceptBooking() {
            publication.reserve(booking.slotId());
            booking.accept("provider");
        }
    }
}
